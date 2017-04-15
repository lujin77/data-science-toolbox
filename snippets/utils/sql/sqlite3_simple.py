#-*- coding:utf-8 -*-
import pandas as pd
import sqlite3

'''
using sqlite demo
'''

peoples = ((1, 'lilei'),(2, 'hanmeimei'))


conn = sqlite3.connect("test.sqlite")
with conn:
    # Native interface ops
    cur = conn.cursor()
    # Creating cities and weather tabels
    cur.execute('DROP TABLE IF EXISTS people')
    cur.execute('CREATE TABLE people (id int, name string)')
    #Populating tables with data ...
    cur.executemany("INSERT INTO people VALUES(?,?)", peoples)
    cur.execute("SELECT * from people")
    rows = cur.fetchall()
    cols = [desc[0] for desc in cur.description]
    df = pd.DataFrame(rows, columns=cols)
    for index, row in df.iterrows():
        print "%s, %s" % (row['id'], row['name'])

    # integration with pandas
    df = pd.read_sql_query("SELECT * FROM people", conn)
    df['BIG_NAME'] = df['name'].str.upper()
    df.to_sql("people", conn, if_exists="replace")
    df = pd.read_sql_query("SELECT * FROM people", conn)
    print df
