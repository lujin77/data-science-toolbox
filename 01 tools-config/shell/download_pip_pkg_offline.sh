#!/bin/bash

'''
离线下载pip安装包，并通过跳板机传递到线上服务器
'''

BRIGDE_SERVER=XX.XX.XX.XX
TARGET_SERVER=XX.XX.XX.XX
USER=lujin

if [ $# != 1 ] ; then
        echo "USAGE: $0 package"
        echo " e.g.: $0 pandas"
        exit 1;
fi

echo "[TRACE] down load pip package ..."
if [ ! -d "pip_$1" ]; then
  mkdir pip_$1
else
  echo "[INFO] pip_dir exist, will be delete"
  rm -fr pip_$1
fi

pip install --download pip_$1/ $1

echo "[TRACE] send to remote server $1 ${USER}@${BRIGDE_SERVER} ..."
scp -r pip_$1 ${USER}@${BRIGDE_SERVER}:/home/${USER}/

echo "[TRACE] scp $1 to ${USER}@${TARGET_SERVER} ..."
ssh -t -p 22 ${USER}@${BRIGDE_SERVER} "scp -r pip_$1 ${USER}@${TARGET_SERVER}:/home/${USER}/"
ssh -t -p 22 ${USER}@${BRIGDE_SERVER} "rm -fr pip_$1"

echo "[NOTICE] all is done"
