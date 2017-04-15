import pandas as pd
import numpy as np
import prettyplotlib
import time
%matplotlib inline

sample_dates = [20161010, 20161011, 20161012]

# 加载hive导出的数据
basePath = "/home/lujin/notebook/data/"
fileName = "XXX.csv"

titles = ['A', 'B', 'C']

s = time.time()
baseDF = pd.read_csv(basePath + fileName, sep=',', names=titles, index_col=False, na_values="null")
print "size:", baseDF.size
print "load time: ", time.time() - s, "sec"
print baseDF.info()

# 按天采样
sampleDF = baseDF[baseDF.dt.isin([20161010, 20161011, 20161012])]
print "total sample -> size=", sampleDF.size

# 过滤完成时间早于开始时间的订单
print "time abnormity (end < start) -> size=", sampleDF[sampleDF.create_time > sampleDF.end_time].size
sampleDF = sampleDF[sampleDF.create_time < sampleDF.end_time]

# 按服务时长是否大于30mins，给订单打上tag
print "time abnormity (create > start) -> size=", sampleDF[sampleDF.create_time > sampleDF.start_time].size
sampleDF = sampleDF[sampleDF.create_time < sampleDF.start_time]

# filter service_order which over half hour, add tag
is_over_half_hour = (sampleDF.start_time - sampleDF.create_time) > (60 * 30)
print "over half hour -> size:", sampleDF[is_over_half_hour].size
sampleDF['gt_30mins'] = is_over_half_hour

# int转换为timestamp，修正时区问题（手动+8）
sampleDF['ts_create_time'] = pd.to_datetime(sampleDF['create_time'], unit='s', utc=True) + pd.Timedelta(hours=8)
sampleDF['ts_arrival_time'] = pd.to_datetime(sampleDF['arrival_time'], unit='s', utc=True) + pd.Timedelta(hours=8)
sampleDF['ts_start_time'] = pd.to_datetime(sampleDF['start_time'], unit='s', utc=True) + pd.Timedelta(hours=8)
sampleDF['ts_end_time'] = pd.to_datetime(sampleDF['end_time'], unit='s', utc=True) + pd.Timedelta(hours=8)
sampleDF[sampleDF.order_status==7][['create_time', 'ts_create_time', 'ts_arrival_time', 'ts_start_time', 'ts_end_time', 'gt_30mins']].head()


# 按30分钟聚合函数
def agg_half_hours(df, dt):
    ts_series_30mins = df.groupby('ts_create_time').size().resample('30T').sum()
    # 过滤异常日期
    ts_series_30mins_ok = ts_series_30mins[ts_series_30mins.index.strftime("%Y%m%d")==str(dt)]
    return ts_series_30mins_ok

# 按天计算结果并输出
for dt in [20161010, 20161011, 20161012]:
    resultDF = pd.DataFrame()
    print "[INFO] date=", dt
    #服务时间小余等于30mins
    dailyDF = sampleDF[(sampleDF.dt==dt) & ~sampleDF.gt_30mins]
    resultDF['total_<=30mins'] = agg_half_hours(dailyDF, dt)
    #服务时间大余30mins
    dailyDF = sampleDF[(sampleDF.dt==dt) & sampleDF.gt_30mins]
    resultDF['total_>30mins'] = agg_half_hours(dailyDF, dt)
    # dump result
    resultDF.to_csv("/home/lujin/half_hour_binning_{dt}.csv".format(dt=dt))
