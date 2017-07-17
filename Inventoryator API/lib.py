import os
import sys

from flask.ext.sqlalchemy import SQLAlchemy
from webargs.flaskparser import FlaskParser

from datetime import timedelta
from flask import make_response, request, current_app
from functools import update_wrapper

__author__ = 'amccormick'

db = SQLAlchemy()

parser = FlaskParser()


def flask_mysql_config(app):
    try:
        from ConfigParser import RawConfigParser

        cfgparser = RawConfigParser()
        currdir = os.path.dirname(os.path.realpath(__file__))
        with open(currdir + '/install/config.ini', 'r+') as config:
            cfgparser.readfp(config)
        mysql_url = cfgparser.get('mysql', 'URL')
        mysql_port = cfgparser.get('mysql', 'PORT')
        mysql_user = cfgparser.get('mysql', 'USER')
        mysql_pass = cfgparser.get('mysql', 'PASSWORD')
        mysql_schema = cfgparser.get('mysql', 'SCHEMA')
        app.config['SQLALCHEMY_DATABASE_URI'] = 'mysql+mysqlconnector://{}:{}@{}:{}/{}'.format(mysql_user, mysql_pass,
                                                                                               mysql_url, mysql_port,
                                                                                               mysql_schema)
    except Exception as e:
        sys.stderr.write("MySQL DB URL not found. If you are running in a dev environment, please see instructions.\n")
        sys.stderr.flush()
        raise


def db_add(item):
    db.session.add(item)
    if db.session.commit() is None:
        return True
    return False


def db_delete(item):
    db.session.delete(item)
    if db.session.commit() is None:
        return True
    return False


def crossdomain(origin=None, methods=None, headers=None,
                max_age=21600, attach_to_all=True,
                automatic_options=True):
    if methods is not None:
        methods = ', '.join(sorted(x.upper() for x in methods))
    if headers is not None and not isinstance(headers, basestring):
        headers = ', '.join(x.upper() for x in headers)
    if not isinstance(origin, basestring):
        origin = ', '.join(origin)
    if isinstance(max_age, timedelta):
        max_age = max_age.total_seconds()

    def get_methods():
        if methods is not None:
            return methods

        options_resp = current_app.make_default_options_response()
        return options_resp.headers['allow']

    def decorator(f):
        def wrapped_function(*args, **kwargs):
            if automatic_options and request.method == 'OPTIONS':
                resp = current_app.make_default_options_response()
            else:
                resp = make_response(f(*args, **kwargs))
            if not attach_to_all and request.method != 'OPTIONS':
                return resp

            h = resp.headers

            h['Access-Control-Allow-Origin'] = origin
            h['Access-Control-Allow-Methods'] = get_methods()
            h['Access-Control-Max-Age'] = str(max_age)
            if headers is not None:
                h['Access-Control-Allow-Headers'] = headers
            return resp

        f.provide_automatic_options = False
        return update_wrapper(wrapped_function, f)

    return decorator
