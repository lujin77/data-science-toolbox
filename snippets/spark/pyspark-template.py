# -*- coding: utf-8 -*-

import os
import sys

import findspark
findspark.init()

from pyspark import SparkContext
from pyspark import SparkConf
from pyspark.sql import HiveContext

conf = SparkConf().setAppName("APP_NAME").setMaster("yarn-client")
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
