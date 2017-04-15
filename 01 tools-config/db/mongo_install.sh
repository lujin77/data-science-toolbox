mongod --dbpath=/var/tmp/lujin/mongoDB --bind_ip=0.0.0.0 --port=30000

mongod --dbpath=/var/tmp/lujin/mongoDB -logpath=/var/tmp/lujin/mongodb.log --logappend  --bind_ip=0.0.0.0 --port=30000  --directoryperdb --nojournal
