from pyhive import presto
from pyhive import hive

'''
Python 2.7
For Presto: Presto install
For Hive: HiveServer2 daemon

pip install pyhive[hive] for the Hive interface and
pip install pyhive[presto] for the Presto interface.
'''

cursor = presto.connect('localhost').cursor()
cursor.execute('SELECT * FROM DB_NAME LIMIT 10')
#print cursor.fetchone()
print cursor.fetchall()
