from impala.dbapi import connect

'''
using HiveServer2
'''

conn = connect(host='localhost', port=10000)
cursor = conn.cursor()
cursor.execute('SELECT * FROM DB.TABLE LIMIT 1')
print cursor.description  # prints the result set's schema
results = cursor.fetchall()
