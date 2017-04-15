# -*- coding: utf-8 -*-
import pandas as pd
import datetime
import time
from collections import namedtuple


def timestampe_bound(dt="20161010"):
    '''
    根据传入字符串日期获取timestampe上下边界
    '''
    start, end = datetime_bound(dt)
    return int(time.mktime(start.timetuple())), int(time.mktime(end.timetuple()))


def datetime_bound(dt="20161010"):
    '''
    根据传入字符串日期获取datetime上下边界
    '''
    start = datetime.datetime.strptime(dt, "%Y%m%d")
    end = datetime.datetime.combine(start, datetime.time.max)
    return start, end


def getPreDatesStr(delta_days=0):
    '''
    获取最近7天的字符串
    '''
    end = datetime.datetime.now()
    start = (end - datetime.timedelta(days=PRE_DAYS)).strftime("%Y-%m-%d")
    end = end.strftime("%Y-%m-%d")
    # 加载最近7天
    return [dt for dt in pd.date_range(start, end).strftime("%Y%m%d")]

def getMonthBound(dt="20161010"):
    base = datetime.datetime.strptime(dt, "%Y%m%d")
    first = datetime.date(day=1, month=base.month, year=base.year)
    nextMonth = first + datetime.timedelta(days=31)
    last = datetime.date(day=1, month=nextMonth.month, year=nextMonth.year) - datetime.timedelta(days=1)
    return first, last

if __name__ == '__main__':
    start, end = timestampe_bound(dt="20161010")
    print datetime.datetime.fromtimestamp(start), datetime.datetime.fromtimestamp(end)

    start, end = getMonthBound("20161010")
    print start, end
