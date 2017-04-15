%matplotlib inline
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import seaborn as sns
from pandasql import sqldf

# 设置pandas sql
pysqldf = lambda q: sqldf(q, globals())

city = "bj"

# 加载数据
Location = '/home/lujin/notebook/data/{city}_complete.csv'.format(city=city)
baseDF = pd.read_csv(Location)

# 改变dt的类型为datetime
baseDF['dt'] = pd.to_datetime(baseDF.dt.astype(str), format='%Y%m%d')
baseDF['complete_rate'] = baseDF.complete / baseDF.total
baseDF['bidding'] = baseDF.final_bidding_rate.round(1)
del baseDF['final_bidding_rate']
baseDF['week'] = baseDF.expect_start_day_of_week
del baseDF['expect_start_day_of_week']
baseDF['hour'] = baseDF.expect_start_hour_of_day
del baseDF['expect_start_hour_of_day']

# 单独提出加价的DF
#biddingDF = baseDF[baseDF.final_bidding_rate>0]

print "total: %d" % (baseDF.size)

baseDF[(baseDF.bidding<=2.0) & (baseDF.bidding>0)].head()
print pysqldf("select dt, sum(total) from baseDF group by dt order by dt").head()
print baseDF.groupby('dt')['total'].sum().head()
print baseDF.pivot_table(index='dt', values=['total'], aggfunc=np.sum).head()

grouped = "total_batch, price_type, driver_num, wasting_travel_rate, bidding, week, hour"
df = pysqldf("select {grouped}, sum(complete), sum(total) from baseDF where bidding<=0.5 group by {grouped}".format(grouped=grouped))
df['rate'] = df['sum(complete)'] / df['sum(total)']
print df.size
print df[df['sum(total)']>10].size

df = df[df['sum(total)']>10]
del df['sum(complete)']
del df['sum(total)']

print df.shape
print df.dtypes
df.head()

#sns.distplot(df.wasting_travel_rate)

corr = np.corrcoef(df.T)
corr = pd.DataFrame(corr, index=df.columns, columns=df.columns)
mask = np.zeros_like(corr)
mask[np.triu_indices_from(mask)] = True
sns.heatmap(corr, mask=mask, vmax=.3, square=True)
