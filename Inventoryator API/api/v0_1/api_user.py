from flask import Blueprint, jsonify, make_response, request
from sqlalchemy.exc import IntegrityError
from webargs import Arg, ValidationError
from api.v0_1.api_lib import password_length_validator, verify_user
from lib import db, parser, db_add, db_delete
from models.inventory import User, Inventory
from serializers.inventory import UserSchema, InventorySchema

user_api = Blueprint('user', __name__)


@user_api.route('/user/', methods=['GET'])
def get_all_users():
    users = db.session.query(User).all()
    if len(users) == 0:
        return make_response(jsonify({'data': 'No users found.'}), 200)
    return make_response(jsonify({'success': True, 'data': UserSchema(users, many=True).data}), 200)


@user_api.route('/login/', methods=['PUT'])
def login_user():
    user_args = {
        'username': Arg(str, required=True),
        'password': Arg(str, required=True, validate=password_length_validator, use=lambda val: val.lower())
    }
    print request.json

    args = parser.parse(user_args, request)
    if verify_user(args['username'], args['password']) is False:
        return make_response(jsonify({'success': False, 'error': 'Password incorrect'}), 401)
    return make_response(jsonify({'success': True, 'data': 'Valid user.'}), 200)


@user_api.route('/user/', methods=['POST'])
def register_user():
    user_args = {
        'username': Arg(str, required=True),
        'password1': Arg(str, validate=password_length_validator, required=True, use=lambda val: val.lower()),
        'password2': Arg(str, validate=password_length_validator, required=True, use=lambda val: val.lower())
    }

    args = parser.parse(user_args, request)

    user = User()
    user.username = args['username']
    if args['password1'] == args['password2']:
        user.password = args['password1']
        try:
            if db_add(user):
                return make_response(jsonify({'success': True, 'data': UserSchema(user).data}), 201)
        except IntegrityError as e:
            return make_response(jsonify({'success': False, 'error': 'Username already exists.'}))
    else:
        return make_response(jsonify({'success': False, 'error': 'Passwords do not match'}), 400)

    return make_response(jsonify({'success': False, 'error': 'User {} not added'.format(args['username'])}), 500)


@user_api.route('/user/<string:username>/', methods=['GET'])
def get_user(username):
    user = db.session.query(User).get(username)
    if user is None:
        return make_response(jsonify({'data': 'User {} not found.'.format(username)}), 200)
    return make_response(jsonify({'success': True, 'data': UserSchema(user).data}), 200)


@user_api.route('/user/<string:username>/', methods=['PUT'])
def update_password(username):
    user = db.session.query(User).get(username)
    if user is None:
        return make_response(jsonify({'data': 'User {} not found.'.format(username)}), 200)

    verify_args = {
        'password': Arg(str, validate=password_length_validator, required=True, use=lambda val: val.lower())
    }
    args = parser.parse(verify_args, request)
    if verify_user(username, args['password']) is False:
        return make_response(jsonify({'success': False, 'error': 'Password incorrect'}), 401)

    user_args = {
        'password1': Arg(str, validate=password_length_validator, required=True, use=lambda val: val.lower()),
        'password2': Arg(str, validate=password_length_validator, required=True, use=lambda val: val.lower())
    }

    args = parser.parse(user_args, request)

    if args['password1'] == args['password2']:
        user.password = args['password1']

        if db_add(user):
            return make_response(jsonify({'success': True, 'data': 'Password for {} updated.'.format(username)}), 200)
        return make_response(jsonify({'success': False, 'error': 'User {} not updated'.format(username)}))
    else:
        raise ValidationError('Passwords need to match.')


@user_api.route('/user/<string:username>/share/<string:share_code>/', methods=['PUT'])
def share_inventory(username, share_code):
    user = db.session.query(User).get(username)

    if user is None:
        return make_response(jsonify({'success': False, 'data': 'User {} not found.'.format(username)}), 404)

    verify_args = {
        'password': Arg(str, validate=password_length_validator, required=True, use=lambda val: val.lower())
    }
    args = parser.parse(verify_args, request)
    if verify_user(username, args['password']) is False:
        return make_response(jsonify({'success': False, 'error': 'Password incorrect'}), 401)

    inventory = db.session.query(Inventory).filter(Inventory.share_code == share_code).first()

    if inventory is None:
        return make_response(jsonify({'success': False, 'data': 'Inventory for code:{} not found.'.format(share_code)}),
                             404)

    user.inventories.append(inventory)

    if db_add(user):
        return make_response(jsonify({'success': True, 'data': UserSchema(user).data}), 201)

    return make_response(
        jsonify({'success': False, 'error': 'Inventory for code:{} not added to user {}'.format(share_code, username)}))


@user_api.route('/user/<string:username>/inventory/', methods=['GET'])
def get_user_inventories(username):
    user = db.session.query(User).get(username)

    if user is None:
        return make_response(jsonify({'data': 'User {} not found.'.format(username)}), 200)

    verify_args = {
        'password': Arg(str, validate=password_length_validator, required=True, use=lambda val: val.lower())
    }
    args = parser.parse(verify_args, request)
    if verify_user(username, args['password']) is False:
        return make_response(jsonify({'success': False, 'error': 'Password incorrect'}), 401)

    return make_response(jsonify({'success': True, 'data': InventorySchema(user.inventories, many=True).data}))


@user_api.route('/user/<string:username>/inventory/<int:inventory_id>/', methods=['PUT'])
def add_inventory(username, inventory_id):
    user = db.session.query(User).get(username)

    if user is None:
        return make_response(jsonify({'data': 'User {} not found.'.format(username)}), 200)

    verify_args = {
        'password': Arg(str, validate=password_length_validator, required=True, use=lambda val: val.lower())
    }
    args = parser.parse(verify_args, request)
    if verify_user(username, args['password']) is False:
        return make_response(jsonify({'success': False, 'error': 'Password incorrect'}), 401)

    inventory = db.session.query(Inventory).get(inventory_id)

    if inventory is None:
        return make_response(jsonify({'data': 'Inventory id:{} not found.'.format(inventory_id)}), 200)

    user.inventories.append(inventory)

    if db_add(user):
        return make_response(jsonify({'success': True, 'data': UserSchema(user).data}), 201)

    return make_response(
        jsonify({'success': False, 'error': 'Inventory id:{} not added to user {}'.format(inventory_id, username)}))


@user_api.route('/user/<string:username>/inventory/<int:inventory_id>/', methods=['DELETE'])
def remove_inventory(username, inventory_id):
    user = db.session.query(User).get(username)

    if user is None:
        return make_response(jsonify({'data': 'User {} not found.'.format(username)}), 200)

    verify_args = {
        'password': Arg(str, validate=password_length_validator, required=True, use=lambda val: val.lower())
    }
    args = parser.parse(verify_args, request)
    if verify_user(username, args['password']) is False:
        return make_response(jsonify({'success': False, 'error': 'Password incorrect'}), 401)

    inventory = db.session.query(Inventory).get(inventory_id)

    if inventory is None:
        return make_response(jsonify({'data': 'Inventory id:{} not found.'.format(inventory_id)}), 200)

    if inventory in user.inventories:
        user.inventories.remove(inventory)
    else:
        return make_response(jsonify(
            {'success': False,
             'error': 'User {} not authorized to access Inventory id:{}'.format(username, inventory_id)}))

    if inventory.users is None:
        if db_delete(inventory):
            return make_response(jsonify({'success': True, 'data': UserSchema(user).data}), 201)

    if db_add(user):
        return make_response(jsonify({'success': True, 'data': UserSchema(user).data}), 201)

    return make_response(
        jsonify({'success': False, 'error': 'Inventory id:{} not added to user {}'.format(inventory_id, username)}))


# webargs flaskparser error handling
@user_api.errorhandler(422)
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
