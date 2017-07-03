#!/usr/bin/env python
# -*- coding: utf-8 -*-

import os
import sys
from optparse import OptionParser

import findspark

findspark.init()

from pyspark import SparkContext
from pyspark import SparkConf
from pyspark.sql import HiveContext


def init_spark(appName, minExecutors, maxExecutors, cores, memory):
    if minExecutors < 10 or maxExecutors < minExecutors:
        raise ValueError("maxExecutors must >= minExecutors, and both >= 10")
    if cores < 1:
        raise ValueError("number of cores in executor must >= 1")
    if


    conf = SparkConf().setAppName("SampleSpliter").setMaster("yarn-client")
    conf.set("driver-memory", "2g")
    conf.set("driver-memory", "2g")
    conf.set("spark.dynamicAllocation.enabled", "true")
    conf.set("spark.dynamicAllocation.minExecutors", "4")
    conf.set("spark.dynamicAllocation.maxExecutors", "12")
    conf.set("spark.executor.cores", "6")
    conf.set("spark.executor.memory", "10g")
    conf.set("spark.default.parallelism", "1024")
    conf.set("spark.yarn.queue", "YOUR.profile")
    sc = SparkContext(conf=conf)
    sqlContext = HiveContext(sc)


def parse_args():
    parser = OptionParser(usage="%prog --input SRC --train out1 --test out2", version="%prog 1.0")
    parser.add_option("--input", type=str, dest="input",
                      help=u"[required] 待分割的数据集", metavar="HDFS_PATH")
    parser.add_option("--train", type=str, dest="train_out",
                      help=u"[required] 分割后的训练集输出的HDFS路径", metavar="HDFS_PATH")
    parser.add_option("--test", type=str, dest="test_out",
                      help=u"[required] 分割后的测试集输出的HDFS路径", metavar="HDFS_PATH")
    parser.add_option("--train_pct", type=float, dest="train_pct", default=0.7,
                      help=u"[optional] 训练集占比（默认0.7）", metavar="PERCENT")
    parser.add_option("-o", "--overwrite",
                      action="store_true", dest="overwrite", default=False,
                      help=u"[optional] 是否强制覆盖输出文件夹（默认false）")
    parser.add_option("-v", "--verbose",
                      action="store_true", dest="verbose", default=False,
                      help=u"[optional] 是否输出详细信息（默认false）")

    # 下述是pyspark参数
    parser.add_option("--queue", type=str,
                      help=u"[required] yarn 队列", metavar="QUEUE_NAME")
    parser.add_option("--minExecutors", type=int, dest="minExecutors", default=10,
                      help=u"spark worker 最小进程数（默认10)", metavar="NUM")
    parser.add_option("--maxExecutors", type=int, dest="maxExecutors", default=100,
                      help=u"spark worker 最大进程数（默认100）", metavar="NUM")
    parser.add_option("--cores", type=int, dest="cores", default=4,
                      help=u"spark worker 核数（默认4）", metavar="NUM")
    parser.add_option("--executorMemory", type=int, dest="executorMemory", default=4,
                      help=u"spark worker 核数（默认4）", metavar="NUM")
    parser.add_option("--parallelism", type=int, dest="parallelism", default=1000,
                      help=u"spark 并行度（默认1000）", metavar="NUM")

    # 指定必填参数
    required_list = ["input", "train_out", "test_out", "queue"]

    # 入参检验
    (options, args) = parser.parse_args()
    for r in required_list:
        if options.__dict__[r] is None:
            parser.error("parameter '--%s XX' required" % r)

    return options


if __name__ == "__main__":
    opt = parse_args()
    print opt
