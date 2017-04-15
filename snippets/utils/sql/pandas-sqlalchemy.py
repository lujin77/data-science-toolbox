import pandas as pd
from sqlalchemy import create_engine, MetaData, Table, select, engine
from sqlalchemy.types import Integer, String, Date
from pandas.io import sql
import sqlalchemy as sa

# testing data generation
ranges = range(0, 10)
df = pd.DataFrame(data=map((lambda x : (x, "user_"+str(x), "2016-12-04")), ranges),
                  index=ranges, columns=["id", "name", "date"])

#engine = create_engine('sqlite://///home/lujin/notebook/test.db')
engine = create_engine('sqlite:///test.db')

# basic ops of pandas with sqlalchemy
df.to_sql('data_chunked', engine,
          dtype={'id': Integer, 'name' : String, 'date' : String},
          index=False, if_exists="replace", chunksize=1000)
# read from DB and cast string to datetime
df = pd.read_sql_table('data_chunked', engine,
                       index_col='id', columns=['name', 'date'],
                       parse_dates={'date': '%Y-%m-%d'})


# ops by chunks
for chunk in pd.read_sql_query("SELECT * FROM data_chunked limit 3", engine, chunksize=1):
    print(chunk)

# using prepared params
df = pd.read_sql(sa.text('SELECT * FROM data_chunked where id=:id'), engine, params={'id': "10"})

# using pandas.io, This is useful for queries that don’t return values
sql.execute('SELECT * FROM data_chunked', engine) # return ResultProxy
sql.execute('INSERT INTO data_chunked VALUES(?, ?, ?)', engine, params=[(11, "lujin", "2012-12-21")])
