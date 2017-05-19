# -*- coding: utf-8 -*-

# step 1. 对要预测的目标数据 y 有一个宏观的把握
df_train['Y'].describe()

# step 2. 通过 Correlation matrix 观察哪些变量会和预测目标关系比较大，哪些变量之间会有较强的关联
corrmat = df_train.corr()
f, ax = plt.subplots(figsize=(12, 9))
sns.heatmap(corrmat, vmax=.8, square=True)
# 进一步分析权重最高的feature的关系
k = 10  # number of variables for heatmap
cols = corrmat.nlargest(k, 'SalePrice')['SalePrice'].index
cm = np.corrcoef(df_train[cols].values.T)
sns.set(font_scale=1.25)
hm = sns.heatmap(cm, cbar=True, annot=True, square=True, fmt='.2f', annot_kws={
                 'size': 10}, yticklabels=cols.values, xticklabels=cols.values)
plt.show()

# step 3. missing value的数量及占比 （不可用特征的过滤）
total = df_train.isnull().sum().sort_values(ascending=False)
percent = (df_train.isnull().sum() / df_train.isnull().count()
           ).sort_values(ascending=False)
missing_data = pd.concat([total, percent], axis=1, keys=['Total', 'Percent'])
missing_data.head(20)
# dealing with missing data
df_train = df_train.drop((missing_data[missing_data['Total'] > 1]).index, 1)
df_train = df_train.drop(df_train.loc[df_train['Electrical'].isnull()].index)

# step 4. outlier 分析 （观察y ~ xi 的分布情况）
var = 'feature_name'
data = pd.concat([df_train['Y'], df_train[var]], axis=1)
data.plot.scatter(x=var, y='Y', ylim=(0, 800000))
# deleting points （假设是最后几个极端值为outlier）
outlier_num = 2
df_train.sort_values(by=var, ascending=False)[:outlier_num]
> 1299，524
df_train = df_train.drop(df_train[df_train['Id'] == 1299].index)
df_train = df_train.drop(df_train[df_train['Id'] == 524].index)

# step 5. 把不符合正态分布的变量给转化成正态分布的 （很重要）
# 作图对比标准正态分布和t分布的对比情况
sns.distplot(df_train['SalePrice'], fit=norm)
fig = plt.figure()
res = stats.probplot(df_train['SalePrice'], plot=plt)
# 采用对数方式转换 （一般情况下能取得较好的效果）
df_train['SalePrice'] = np.log(df_train['SalePrice'])

# step 6. 特征加工
# 1. 数值变类别型
train["CategoryMonth"] = train.Month.replace({1 : "Jan", 2 : "Feb", 3 : "Mar", 4 : "Apr", 5 : "May", 6 : "Jun", 7 : "Jul", 8 : "Aug", 9 : "Sep", 10 : "Oct", 11 : "Nov", 12 : "Dec"})
# 2. 类别型加顺序 （有次序的类别，转换为数值，比如rating）
train["RatingLevel"] = train.Level.replace({"Sal" : 1, "Sev" : 2, "Maj2" : 3, "Maj1" : 4, "Mod": 5, "Min2" : 6, "Min1" : 7, "Typ" : 8})
# 3. 简化类别  (合并类目)
train["SimplCategory"] = train.Category.replace(
    {1: 1, 2: 1,  # bad
     3: 2, 4: 2,  # major
     5: 3, 6: 3, 7: 3,  # minor
     8: 4  # typica
     })
# 4. 多项式扩展 （一般是 2次项，3次项，开平方）
train["OverallQual-s2"] = train["OverallQual"] ** 2
train["OverallQual-s3"] = train["OverallQual"] ** 3
train["OverallQual-Sq"] = np.sqrt(train["OverallQual"])
# 5. 加减乘除的数学关系构造
train["OverallGrade"] = train["OverallQual"] * train["OverallCond"]
# 6. 变为 one－hot
df_train = pd.get_dummies(df_train)
