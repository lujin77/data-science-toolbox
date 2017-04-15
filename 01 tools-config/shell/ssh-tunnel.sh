#!/bin/bash

# chrome
ssh -N -f  -D 7070 跳板机IP

# mysql
ssh -N -f -L 3306:目的服务器IP:3306 跳板机IP

# hive thrifts
ssh -N -f -L 10000:目的服务器IP:10000 跳板机IP

# redis
ssh -N -f -L 6379:目的服务器IP:6379 跳板机IP
