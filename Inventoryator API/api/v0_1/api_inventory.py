import datetime
import random
import string
from flask import Blueprint, make_response, jsonify, request
from webargs import Arg
from api.v0_1.api_lib import password_length_validator, verify_user
from lib import parser, db_add, db_delete, db
from models.inventory import Inventory, Item, User
from serializers.inventory import InventorySchema, ItemSchema

__author__ = 'amccormick'

inventory_api = Blueprint('inventory', __name__)


def verify_user_access(username, inventory):
    user = db.session.query(User).get(username)
    if user is None:
        return False
    if inventory in user.inventories:
        return True
    return False


@inventory_api.route('/inventory/', methods=['GET'])
def get_inventories():
    inventories = db.session.query(Inventory).all()
    if len(inventories) == 0:
        return make_response(jsonify({'data': 'No inventories found.'}), 200)
    return response_success(inventory_schema_simple(inventories))


@inventory_api.route('/inventory/<int:id>/', methods=['GET'])
def get_inventory(id):
    verify_args = {
        'password': Arg(str, validate=password_length_validator, required=True, use=lambda val: val.lower()),
        'username': Arg(str, required=True)
    }
    args = parser.parse(verify_args, request)
    if verify_user(args['username'], args['password']) is False:
        return make_response(jsonify({'success': False, 'error': 'Password incorrect'}), 401)

    inventory = db.session.query(Inventory).get(id)

    if inventory is None:
        return response_not_found(Inventory.__name__, id)

    if verify_user_access(args['username'], inventory):
        return response_success(inventory_schema_simple(inventory))

    return make_response(jsonify(
        {'success': False, 'error': 'User {} not authorized to access Inventory id:{}'.format(args['username'], id)}))


@inventory_api.route('/inventory/', methods=['POST'])
def make_inventory():
    verify_args = {
        'password': Arg(str, validate=password_length_validator, required=True, use=lambda val: val.lower()),
        'username': Arg(str, required=True)
    }
    args = parser.parse(verify_args, request)

    print args

    user = verify_user(args['username'], args['password'])
    if user is False:
        return make_response(jsonify({'success': False, 'error': 'Password incorrect'}), 401)

    username = args['username']

    request_args = {
        'name': Arg(str, required=True),
        'description': Arg(str)
    }

    args = parser.parse(request_args, request)

    inventory = Inventory()
    inventory.name = args['name']
    inventory.description = args['description']
    inventory.updated = datetime.datetime.utcnow()

    user.inventories.append(inventory)

    if db_add(inventory) is True:
        if db_add(user) is True:
            return response_success(inventory_schema_simple(inventory), 201)
        db_delete(inventory)
        return make_response(
            jsonify({'success': False, 'error': 'Inventory was not added to user {}'.format(username)}))

    return make_response(jsonify({'success': False, 'error': 'Inventory {} was not created'.format(args['name'])}), 400)


@inventory_api.route('/inventory/<int:id>/share/', methods=['GET'])
def generate_share_code(id):
    verify_args = {
        'password': Arg(str, validate=password_length_validator, required=True, use=lambda val: val.lower()),
        'username': Arg(str, required=True)
    }
    args = parser.parse(verify_args, request)
    if verify_user(args['username'], args['password']) is False:
        return make_response(jsonify({'success': False, 'error': 'Password incorrect'}), 401)

    inventory = db.session.query(Inventory).get(id)

    if inventory is None:
        return response_not_found(Inventory.__name__, id)

    if verify_user_access(args['username'], inventory):
        code = ''.join(random.choice(string.ascii_uppercase + string.digits) for x in range(8))
        inventory.share_code = code

        if db_add(inventory):
            return response_success(InventorySchema(inventory, only=('id', 'share_code')).data)
        return make_response(
            jsonify({'success': False, 'error': 'Share code not generated for Inventory id:{}'.format(id)}))

    return make_response(jsonify(
        {'success': False, 'error': 'User {} not authorized to access Inventory id:{}'.format(args['username'], id)}))


@inventory_api.route('/inventory/<int:id>/', methods=['PUT'])
def edit_inventory(id):
    verify_args = {
        'password': Arg(str, validate=password_length_validator, required=True, use=lambda val: val.lower()),
        'username': Arg(str, required=True)
    }
    args = parser.parse(verify_args, request)
    username = args['username']
    if verify_user(username, args['password']) is False:
        return make_response(jsonify({'success': False, 'error': 'Password incorrect'}), 401)

    request_args = {
        'name': Arg(str, allow_missing=True),
        'description': Arg(str, allow_missing=True)
    }

    args = parser.parse(request_args, request)
    print args

    if len(args) == 0:
        return make_response(jsonify({'success': False, 'data': 'No changes provided'}))

    inventory = get_inventory_from_db(id)
    if inventory is None:
        return response_not_found(Inventory.__name__, id)

    if verify_user_access(username, inventory):
        if 'name' in args:
            inventory.name = args['name']
        if 'description' in args:
            inventory.description = args['description']
        inventory.updated = datetime.datetime.utcnow()

        if db_add(inventory) is True:
            return response_success(inventory_schema_simple(inventory))

        return make_response(jsonify({'success': False, 'data': 'Inventory {} was not updated'.format(inventory.name)}))

    return make_response(jsonify(
        {'success': False, 'error': 'User {} not authorized to access Inventory id:{}'.format(username, id)}))


@inventory_api.route('/inventory/<int:id>/items/', methods=['POST'])
def add_item(id):
    verify_args = {
        'password': Arg(str, validate=password_length_validator, required=True, use=lambda val: val.lower()),
        'username': Arg(str, required=True)
    }
    args = parser.parse(verify_args, request)
    if verify_user(args['username'], args['password']) is False:
        return make_response(jsonify({'success': False, 'error': 'Password incorrect'}), 401)
    username = args['username']

    request_args = {
        'name': Arg(str, required=True),
        'number': Arg(int, required=True)
    }

    args = parser.parse(request_args, request)

    item = Item()
    # verify inventory
    inventory = get_inventory_from_db(id)
    if inventory is None:
        return response_not_found(Inventory.__name__, id)
    if verify_user_access(username, inventory):
        item.inventory_id = inventory.id
        item.name = args['name']
        item.number = args['number']

        if db_add(item) is True:
            return response_success(ItemSchema(item).data)
        return make_response(jsonify({'success': False, 'data': 'Item {} not added'.format(item.name)}))

    return make_response(jsonify(
        {'success': False, 'error': 'User {} not authorized to access Inventory id:{}'.format(username, id)}))


@inventory_api.route('/inventory/<int:id>/items/', methods=['GET'])
def get_items(id):
    verify_args = {
        'password': Arg(str, validate=password_length_validator, required=True, use=lambda val: val.lower()),
        'username': Arg(str, required=True)
    }
    args = parser.parse(verify_args, request)
    if verify_user(args['username'], args['password']) is False:
        return make_response(jsonify({'success': False, 'error': 'Password incorrect'}), 401)
    username = args['username']

    inventory = get_inventory_from_db(id)
    if inventory is None:
        return response_not_found(Inventory.__name__, id)
    if verify_user_access(username, inventory):
        return response_success(ItemSchema(inventory.items, many=True).data)

    return make_response(jsonify(
        {'success': False, 'error': 'User {} not authorized to access Inventory id:{}'.format(username, id)}))


def response_not_found(type, id, code=404):
    return make_response(jsonify({'success': False, 'data': '{} with id: {} not found'.format(type, id)}), code)


def response_success(data, code=200):
    return make_response(jsonify({'success': True, 'data': data}), code)


def get_inventory_from_db(id):
    return db.session.query(Inventory).get(id)


def get_item_from_db(id):
    return db.session.query(Item).get(id)


def inventory_schema_simple(inventory):
    if type(inventory) == list:
        return InventorySchema(inventory, many=True, only=('id', 'name', 'description')).data
    return InventorySchema(inventory, only=('id', 'name', 'description')).data


# webargs flaskparser error handling
@inventory_api.errorhandler(422)
def handle_bad_request(err):
    # webargs attaches additional metadata to the `data` attribute
    data = getattr(err, 'data')
    if data:
        err_message = data['message']
    else:
        err_message = 'Invalid request'
    return jsonify({
        'error': err_message,
    }), 422
