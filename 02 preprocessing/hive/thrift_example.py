import sys

from hive import ThriftHive
from hive.ttypes import HiveServerException

from thrift import Thrift
from thrift.transport import TSocket
from thrift.transport import TTransport
from thrift.protocol import TBinaryProtocol

try:
  transport = TSocket.TSocket('localhost', 10000)
  transport = TTransport.TBufferedTransport(transport)
  protocol = TBinaryProtocol.TBinaryProtocol(transport)
  client = ThriftHive.Client(protocol)
  transport.open()
  client.execute("SELECT * FROM DB_NAME limit 1")
  while (1):
    row = client.fetchOne()
    if (row == None):
       break
    print row

  #client.execute("SELECT * FROM r")
  #print client.fetchAll()
  transport.close()
except Thrift.TException, tx:
  print '%s' % (tx.message)
