__author__ = 'amccormick'

from marshmallow import Schema, fields


class InventorySchema(Schema):
    list_items = fields.Nested('ItemSchema', exclude=('inventory',), many=True)

    class Meta:
        additional = ['id', 'name', 'description', 'share_code']


class ItemSchema(Schema):
    class Meta:
        additional = ['id', 'name', 'number']


class UserSchema(Schema):
    inventories = fields.Nested('InventorySchema', only=('name', 'id'), many=True)

    class Meta:
        additional = ['username']
