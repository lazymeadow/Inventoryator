"""adding association table for inventories and users

Revision ID: 3eafe784a115
Revises: 29122f6fce33
Create Date: 2015-12-08 04:01:26.490000

"""

# revision identifiers, used by Alembic.
revision = '3eafe784a115'
down_revision = '29122f6fce33'
branch_labels = None
depends_on = None

from alembic import op
import sqlalchemy as sa


def upgrade():
    ### commands auto generated by Alembic - please adjust! ###
    op.create_table('user',
    sa.Column('username', sa.String(length=64), nullable=False),
    sa.Column('password', sa.String(length=64), nullable=True),
    sa.PrimaryKeyConstraint('username')
    )
    op.create_table('has_access',
    sa.Column('username', sa.String(length=64), nullable=False),
    sa.Column('inventory_id', sa.Integer(), nullable=False),
    sa.ForeignKeyConstraint(['inventory_id'], ['inventory.id'], ),
    sa.ForeignKeyConstraint(['username'], ['user.username'], ),
    sa.PrimaryKeyConstraint('username', 'inventory_id')
    )
    op.add_column(u'inventory', sa.Column('share_code', sa.String(length=8), nullable=True))
    op.add_column(u'inventory', sa.Column('updated', sa.DateTime(), nullable=True))
    op.create_unique_constraint(None, 'inventory', ['share_code'])
    ### end Alembic commands ###


def downgrade():
    ### commands auto generated by Alembic - please adjust! ###
    op.drop_constraint(None, 'inventory', type_='unique')
    op.drop_column(u'inventory', 'updated')
    op.drop_column(u'inventory', 'share_code')
    op.drop_table('has_access')
    op.drop_table('user')
    ### end Alembic commands ###