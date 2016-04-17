from flask import Flask, render_template

from lib import flask_mysql_config

app = Flask(__name__)

flask_mysql_config(app)

from lib import db

db.init_app(app)

from api.v0_1.api_inventory import inventory_api
from api.v0_1.api_item import item_api
from api.v0_1.api_user import user_api

app.register_blueprint(inventory_api)
app.register_blueprint(item_api)
app.register_blueprint(user_api)


@app.route('/', methods=['GET'])
def guide():
    return render_template('index.html')


if __name__ == '__main__':
    app.run(debug=True)
