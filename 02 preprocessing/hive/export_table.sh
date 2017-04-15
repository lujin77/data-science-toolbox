#!/bin/bash

if [ $# != 1 ] ; then
        echo "USAGE: $0 TABLE_NAME"
        echo " e.g.: $0 stores"
        exit 1;
fi

if [ -d '/home/lujin/output' ]; then
    echo "[TRACE] clear dir /home/lujin/output"
    rm -fr /home/lujin/output
    mkdir /home/lujin/output
fi

echo "[TRACE] insert overwrite local directory '/home/lujin/output' row format delimited fields terminated by ',' select * from $1"
hive -e "insert overwrite local directory '/home/lujin/output' row format delimited fields terminated by ',' select * from $1"

echo "[TRACE] merge output in /home/lujin/output/* to out."
cat /home/lujin/output/* > $1.csv

rm -fr /home/lujin/output

echo "[NOTICE] all is done"
