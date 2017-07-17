from flask import Blueprint, make_response, jsonify, request
from webargs import fields

from lib import db, db_delete, db_add, parser, crossdomain
from models.inventory import Item
from serializers.inventory import ItemSchema

item_api = Blueprint('item', __name__)


@item_api.route('/item/<int:id>/', methods=['GET'])
@crossdomain(origin='*')
def get_item(id):
    item = get_item_from_db(id)
    if item is None:
        return response_not_found(Item.__name__, id)
    return response_success(ItemSchema().dump(item))


@item_api.route('/item/<int:id>/', methods=['PUT'])
@crossdomain(origin='*')
def edit_item(id):
    request_args = {
        'name': fields.Str(allow_missing=True),
        'number': fields.Int(allow_missing=True)
    }

    args = parser.parse(request_args, request)

    if len(args) == 0:
        return make_response(jsonify({'success': False, 'data': 'No changes provided'}))

    item = get_item_from_db(id)
    if item is None:
        return response_not_found(Item.__name__, id)

    if 'name' in args:
        item.name = args['name']
    if 'number' in args:
        item.number = args['number']

    if db_add(item) is True:
        return response_success(ItemSchema(item).data)

    return make_response(jsonify({'success': False, 'data': 'Inventory {} was not updated'.format(item.name)}))


@item_api.route('/item/<int:id>/', methods=['DELETE'])
@crossdomain(origin='*')
def delete_item(id):
    item = db.session.query(Item).get(id)
    if item is None:
        return response_not_found(Item.__name__, id)
    if db_delete(item) is True:
        return make_response(jsonify({'success': True, 'data': 'Item {} deleted'.format(item.name)}))
    return make_response(jsonify({'success': False, 'data': 'Item {} not deleted'.format(item.name)}))


def response_not_found(type, id, code=404):
    return make_response(jsonify({'success': False, 'data': '{} with id: {} not found'.format(type, id)}), code)


def response_success(data, code=200):
    return make_response(jsonify({'success': True, 'data': data}), code)


def get_item_from_db(id):
    return db.session.query(Item).get(id)

