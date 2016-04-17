from __future__ import with_statement
from logging.config import fileConfig
import os
import sys
import inspect

from alembic import context
from sqlalchemy import engine_from_config, pool



# this is the Alembic Config object, which provides
# access to the values within the .ini file in use.

config = context.config
try:
    from ConfigParser import RawConfigParser

    cfgparser = RawConfigParser()
    currdir = os.path.dirname(os.path.realpath(__file__))
    with open('install/config.ini', 'r+') as cfg:
        cfgparser.readfp(cfg)
    mysql_url = cfgparser.get('mysql', 'URL')
    mysql_port = cfgparser.get('mysql', 'PORT')
    mysql_user = cfgparser.get('mysql', 'USER')
    mysql_pass = cfgparser.get('mysql', 'PASSWORD')
    mysql_schema = cfgparser.get('mysql', 'SCHEMA')
    config.set_main_option('sqlalchemy.url', 'mysql+mysqlconnector://{}:{}@{}:{}/{}'.format(mysql_user, mysql_pass,
                                                                                            mysql_url, mysql_port,
                                                                                            mysql_schema))
except Exception as e:
    sys.stderr.write("MySQL DB URL not found. If you are running in a dev environment, please see instructions.\n")
    sys.stderr.flush()
    raise

# Interpret the config file for Python logging.
# This line sets up loggers basically.
fileConfig(config.config_file_name)

# add your model's MetaData object here
# for 'autogenerate' support
currentdir = os.path.dirname(os.path.abspath(inspect.getfile(inspect.currentframe())))
parentdir = os.path.dirname(currentdir)
sys.path.insert(0,parentdir)
# from myapp import mymodel
# target_metadata = mymodel.Base.metadata
from inventoryator import db
from models.inventory import Inventory, Item

target_metadata = db.metadata


# other values from the config, defined by the needs of env.py,
# can be acquired:
# my_important_option = config.get_main_option("my_important_option")
# ... etc.


def run_migrations_offline():
    """Run migrations in 'offline' mode.

    This configures the context with just a URL
    and not an Engine, though an Engine is acceptable
    here as well.  By skipping the Engine creation
    we don't even need a DBAPI to be available.

    Calls to context.execute() here emit the given string to the
    script output.

    """
    url = config.get_main_option("sqlalchemy.url")
    context.configure(
        url=url, target_metadata=target_metadata, literal_binds=True)

    with context.begin_transaction():
        context.run_migrations()


def run_migrations_online():
    """Run migrations in 'online' mode.

    In this scenario we need to create an Engine
    and associate a connection with the context.

    """
    connectable = engine_from_config(
        config.get_section(config.config_ini_section),
        prefix='sqlalchemy.',
        poolclass=pool.NullPool)

    with connectable.connect() as connection:
        context.configure(
            connection=connection,
            target_metadata=target_metadata
        )

        with context.begin_transaction():
            context.run_migrations()


if context.is_offline_mode():
    run_migrations_offline()
else:
    run_migrations_online()
