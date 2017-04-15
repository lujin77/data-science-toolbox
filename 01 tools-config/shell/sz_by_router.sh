#!/bin/bash

# 通过跳板机下载文件

BOAT_SERVER="XX.XX.XX.XX"

if [ $# != 1 ] ; then
        echo "USAGE: $0 FILE"
        echo " e.g.: $0 test.dat"
        exit 1;
fi

echo "[TRACE] scp $1 to lujin@$BOAT_SERVER ..."
ssh -t -p 22 lujin@$TARGET_SERVER "rm ~/$1"
scp $1 lujin@$TARGET_SERVER:~/

echo "[TRACE] sz $1 from lujin@$BOAT_SERVER ..."
ssh -t -p 22 lujin@$TARGET_SERVER "sz ~/$1"

echo "[NOTICE] all is done"
