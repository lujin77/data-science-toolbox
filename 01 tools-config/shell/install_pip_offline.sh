#!/bin/bash

'''
使用离线下载的pip进行安装
'''

if [ $# != 1 ] ; then
        echo "USAGE: $0 package"
        echo " e.g.: $0 pandas"
        exit 1;
fi

if [ -d "pip_$1" ]; then
  pip install --no-index --find-links file:/home/lujin/pip_$1 $1
  rm -fr pip_$1
else
  echo "[ERROR] please download pipe file firstly"
fi
