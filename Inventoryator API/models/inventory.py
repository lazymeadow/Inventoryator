__author__ = 'amccormick'

from lib import db


class User(db.Model):
    username = db.Column(db.String(64), primary_key=True)
    password = db.Column(db.String(64))
    inventories = db.relationship('Inventory', secondary='has_access')


class Inventory(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    name = db.Column(db.String(64))
    description = db.Column(db.String(256))
    share_code = db.Column(db.String(8), unique=True)
    updated = db.Column(db.DateTime)
    items = db.relationship('Item', backref='inventory')
    users = db.relationship('User', secondary='has_access')


class HasAccess(db.Model):
    username = db.Column(db.String(64), db.ForeignKey('user.username'), primary_key=True)
    inventory_id = db.Column(db.Integer, db.ForeignKey('inventory.id'), primary_key=True)


class Item(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    name = db.Column(db.String(64))
    number = db.Column(db.Integer)
    inventory_id = db.Column(db.Integer, db.ForeignKey('inventory.id'), index=True)
