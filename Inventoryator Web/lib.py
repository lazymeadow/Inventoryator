import os

import sys
from flask_sqlalchemy import SQLAlchemy
from webargs.flaskparser import FlaskParser

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
