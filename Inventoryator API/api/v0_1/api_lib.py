from marshmallow import ValidationError

from lib import db
from models.inventory import User


def verify_user(username, password):
    if username is None or password is None:
        return False
    user = db.session.query(User).get(username)
    if user is None or user.password != password:
        return False
    return user


def password_length_validator(val):
    if len(val) == 64:
        return True
    raise ValidationError('Bad password length. Passwords should be 64 characters long and hashed with SHA256.')

