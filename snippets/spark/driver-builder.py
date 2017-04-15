'''
spark 2.x spark session construct
'''

from pyspark.sql import SparkSession

spark = SparkSession.builder \
        .master("local[4]") \
        .appName("tmp") \
        .enableHiveSupport() \
        .config("spark.driver.memory", "2g") \
        .config("spark.executor.memory", "1g") \
        .config("spark.default.parallelism", "4") \
        .config("spark.sql.shuffle.partitions", "100") \
        .getOrCreate()

spark.stop()
