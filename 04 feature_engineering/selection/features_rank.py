# -*- coding: utf-8 -*-

'''
特征选择辅助脚本
'''

from functools import wraps
import time
import pandas as pd
import numpy as np
from sklearn.preprocessing import LabelEncoder, StandardScaler, MinMaxScaler
from sklearn_pandas import DataFrameMapper, cross_val_score
from sklearn.datasets import load_iris
from sklearn.ensemble import RandomForestClassifier
from sklearn.feature_selection import SelectKBest
from sklearn.feature_selection import chi2
from scipy.stats import pearsonr
from sklearn.utils import resample
from minepy import MINE


def timeit(func):
    '''
    计时器，Decorator模式

    :param func: 函数指针
    :return: None
    '''

    @wraps(func)
    def wrapper(*args, **kwargs):
        start_time = time.time()
        result = func(*args, **kwargs)
        print "[INFO] {name}() running elasped: {time}s".format(name=func.__name__, time=(time.time() - start_time))
        return result

    return wrapper


@timeit
def chi2_rank(col_names, X, y, topK=10):
    '''
    卡方校验特征重要性检测

    :param col_names: 特征名，list
    :param X: 特征矩阵，numpy 2D array
    :param y: 标签向量，numpy array
    :param topK: 输出前k个变量
    :return: 排序后的特征dataframe，含权重和置信度
    '''

    score, pvalue = chi2(X, y)

    result_df = pd.DataFrame({'name': col_names,
                              'chi2': score.tolist(),
                              'pvalue': pvalue.tolist()})

    result_df = result_df[['name', 'chi2', 'pvalue']].sort_values('chi2', ascending=False)

    print "size={m} features={n} top{k} rank for chi2 testing, :".format(m=len(y), n=len(col_names), k=topK)
    print result_df.head(topK)
    return result_df


@timeit
def pearsonr_rank(col_names, X, y, topK=10):
    '''
    皮尔逊相关系数特征重要性检测

    :param col_names: 特征名，list
    :param X: 特征矩阵，numpy 2D array
    :param y: 标签向量，numpy array
    :param topK: 输出前k个变量
    :return: 排序后的特征dataframe，含权重和置信度
    '''

    scores = []
    pvalues = []
    for i in range(0, len(col_names)):
        rscore, pvalue = pearsonr(X[:, i], y)
        scores.append(rscore)
        pvalues.append(pvalue)

    result_df = pd.DataFrame({'name': col_names,
                              'pearsonr': scores,
                              'pvalue': pvalues})

    result_df = result_df[['name', 'pearsonr', 'pvalue']].sort_values('pearsonr', ascending=False)

    print "size={m} features={n} top{k} rank for pearsonr testing, :".format(m=len(y), n=len(col_names), k=topK)
    print result_df.head(topK)
    return result_df


@timeit
def mutual_infomation_rank(col_names, X, y, topK=10):
    '''
    互信息特征重要性检测

    :param col_names: 特征名，list
    :param X: 特征矩阵，numpy 2D array
    :param y: 标签向量，numpy array
    :param topK: 输出前k个变量
    :return: 排序后的特征dataframe，含权重和置信度
    '''

    # 因为互信息计算较慢，进行采样后再计算
    original_size = len(y)
    sampling_size = 2000 if original_size > 2000 else original_size
    X, y = resample(X, y, random_state=0, n_samples=sampling_size)

    mine = MINE(alpha=0.6, c=15, est="mic_approx")

    scores = []
    for i in range(0, len(col_names)):
        mine.compute_score(X[:, i], y)
        scores.append(mine.mic())

    result_df = pd.DataFrame({'name': col_names,
                              'mutual_information': scores})

    result_df = result_df[['name', 'mutual_information']].sort_values('mutual_information', ascending=False)

    print "size={m} sampling={s} features={n} top{k} rank for MINE testing:" \
        .format(m=original_size, s=sampling_size, n=len(col_names), k=topK)
    print result_df.head(topK)
    return result_df


@timeit
def tree_model_rank(col_names, X, y, topK=10):
    '''
    树模型特征重要性检测

    :param col_names: 特征名，list
    :param X: 特征矩阵，numpy 2D array
    :param y: 标签向量，numpy array
    :param topK: 输出前k个变量
    :return: 排序后的特征dataframe，含权重和置信度
    '''

    # 为减少运算量，进行随机采样
    original_size = len(y)
    sampling_size = 5000 if original_size > 5000 else original_size
    X, y = resample(X, y, random_state=0, n_samples=sampling_size)

    cls = RandomForestClassifier()
    model = cls.fit(X, y)

    result_df = pd.DataFrame({'name': col_names,
                              'tree_importance': model.feature_importances_.tolist()})

    result_df = result_df[['name', 'tree_importance']].sort_values('tree_importance', ascending=False)

    print "size={m} sampling={s} features={n} top{k} rank for RandomForest testing:" \
        .format(m=original_size, s=sampling_size, n=len(col_names), k=topK)
    print result_df.head(topK)
    return result_df


def sampling(X, y, size=1000):
    sampling_size = size if len(y) > size else len(y)
    X, y = resample(X, y, random_state=0, n_samples=sampling_size)
    return X, y


if __name__ == '__main__':
    df = pd.read_csv("/Users/lujin/Downloads/order.csv", index_col=None, nrows=100000)

    features_meta = [
        ('car_type_id', LabelEncoder()),
        ('car_brand', LabelEncoder()),
        ('coupon_name', LabelEncoder()),
        ('coupon_facevalue', LabelEncoder()),
        ('user_type', LabelEncoder()),
        ('dispatch_type', LabelEncoder()),
        ('decision_type', LabelEncoder()),
        ('user_level', LabelEncoder()),
        ('user_gender', LabelEncoder()),
        ('isrushhour', LabelEncoder()),
        ('pre_availabledrivernum', None),
        ('abs_demand', None),
        ('relative_supply', None),
        ('user_order_count', None),
        ('geo_order_count', None),
        ('geo_bidding_count', None),
        ('geo_bidding_pct', None),
    ]

    # 使用pandas-sklearn集成features
    mapper = DataFrameMapper(features_meta, df_out=True)
    X_df = mapper.fit_transform(df)
    y_df = df['label']

    # 提取计算变量
    col_names = X_df.keys()
    X = X_df.values
    y = y_df.values

    # 卡方排序
    chi2_rank(col_names=col_names, X=X, y=y)

    # 皮尔逊排序
    pearsonr_rank(col_names=col_names, X=X, y=y)

    # 互信息排序
    mutual_infomation_rank(col_names=col_names, X=X, y=y)

    # 随机森林排序
    tree_model_rank(col_names=col_names, X=X, y=y)
