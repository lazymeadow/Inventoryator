from flask_assets import Environment
from sqlalchemy.exc import IntegrityError
from _sha256 import sha256

from flask import Flask, render_template, redirect, url_for, flash, jsonify, make_response, request
from flask_login import LoginManager, UserMixin, current_user, login_user, logout_user
from webargs import fields
from webargs.flaskparser import use_args
from webassets import Bundle

from lib import flask_mysql_config, db, db_add, db_delete
from models.inventory import User, Inventory, Item
from serializers.inventory import UserSchema, InventorySchema

app = Flask(__name__)
app.secret_key = 'inventoriesAREpotatoes'

assets = Environment(app)
css_bundle = Bundle('style.less', output='style.css', filters=['less'], depends='**/*.less')
assets.register('css', css_bundle)
js_bundle = Bundle('script.js', output='script.js', depends='**/*.js')
assets.register('js', js_bundle)

flask_mysql_config(app)
db.init_app(app)

login_manager = LoginManager()
login_manager.init_app(app)


class RealUser(UserMixin):
    def __init__(self, username, password, user_model):
        self.username = username
        self.password = password
        self.user = user_model

    def get_id(self):
        return self.username


@login_manager.user_loader
def load_user(user_id):
    user = db.session.query(User).get(user_id)
    return RealUser(user.username, user.password, user)


@app.route('/')
def index():
    if current_user.is_authenticated:
        inventories = UserSchema().dump(db.session.query(User).get(current_user.username)).data['inventories']
        return render_template("index.html", inventories=inventories)
    return redirect(url_for('login'))


@app.route('/login', methods=['GET'])
def login():
    if current_user.is_authenticated:
        redirect(url_for('index'))
    return render_template("login.html")


login_args = {"username": fields.Str(),
              "password": fields.Str()}


@app.route('/login/', methods=['POST'])
@use_args(login_args)
def post_login(args):
    loaded_user = load_user(args['username'])
    if loaded_user.password == sha256(args['password']).hexdigest():
        login_user(loaded_user)
    else:
        flash('Username or Password is invalid', 'error')
    return index()


@app.route('/logout/', methods=['GET'])
def logout():
    logout_user()
    return index()


@app.route('/register/', methods=['GET'])
def registration():
    return render_template("register.html")


register_args = {"username": fields.Str(),
                 "password1": fields.Str(),
                 "password2": fields.Str()}


@app.route('/register/', methods=['POST'])
@use_args(register_args)
def register_new(args):
    # look up the correct way to do this
    if args['password1'] == args['password2']:
        user = User()
        user.username = args['username']
        user.password = sha256(args['password1']).hexdigest()
        try:
            if db_add(user):
                flash('Account created. Please login.', 'success')
                return index()
        except IntegrityError as e:
            flash('Username already taken.', 'error')
    else:
        flash('Passwords do not match', 'error')
    return registration()


@app.route('/inventory/<int:inventory_id>/')
def inventory(inventory_id):
    inventory = InventorySchema().dump(db.session.query(Inventory).get(inventory_id)).data
    return render_template('inventory.html', inventory=inventory)


new_inventory_args = {"name": fields.Str(),
                      "description": fields.Str()}


@app.route('/inventory/', methods=['POST'])
@use_args(new_inventory_args)
def new_inventory(args):
    inventory = Inventory()
    inventory.name = args['name']
    inventory.description = args['description']
    print current_user.user
    current_user.user.inventories.append(inventory)
    db_add(inventory)
    db_add(current_user.user)
    return index()


@app.route('/item/<int:item_id>', methods=['DELETE'])
def delete_items(item_id):
    item = db.session.query(Item).get(item_id)
    if db_delete(item):
        flash('{} deleted.'.format(item.name))
    else:
        flash('{} could not be deleted.'.format(item.name))
    return make_response(jsonify(''))


new_item_args = {"name": fields.Str(),
                 "quantity": fields.Int(),
                 "inventory_id": fields.Int()}


@app.route('/item', methods=['POST'])
@use_args(new_item_args)
def new_item(args):
    item = Item()
    item.name = args['name']
    item.number = args['quantity']
    item.inventory_id = args['inventory_id']
    if db_add(item):
        flash('{} ({}) added.'.format(item.name, item.number))
    else:
        flash('{} could not be added.'.format(item.name))
    return redirect(request.referrer)


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)
