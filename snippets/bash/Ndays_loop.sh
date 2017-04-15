#!/bin/bash

DAYS=7

startdate=`date -d "$DAYS day ago" "+%Y%m%d"`
enddate=`date -d "1 day ago" "+%Y%m%d"`

# 获取日期集合
dates=()
for (( date="$startdate"; date != enddate; )); do
    dates+=( "$date" )
    date="$(date --date="$date + 1 days" +'%Y%m%d')"
done
echo "${dates[@]}"

# 遍历每一天
for dt in "${dates[@]}"; do
    echo $dt
done
