#export SPARK_SUBMIT_OPTIONS="--driver-java-options -Xmx8g"
#export ZEPPELIN_INT_MEM="-Xmx8g"
export ZEPPELIN_JAVA_OPTS="-Ddriver-memory=1G -Dspark.executor.memory=1g -Dspark.cores.max=2 -Dspark.sql.shuffle.partitions=10 -Dspark.local.dir=/Users/lujin/tmp"

#export SCALA_HOME=/home/lujin/software/scala-2.11.8
#export SPARK_HOME=/home/lujin/software/spark-2.0.1-bin-hadoop2.7
export MASTER=local[1]
#export HADOOP_CONF_DIR=$HADOOP_HOME/etc/hadoop

# Found both spark.driver.extraClassPath and SPARK_CLASSPATH. Use only the former.
unset SPARK_CLASSPATH

export CLASSPATH=$CLASSPATH:$SPARK_HOME/lib/mysql-connector-java-5.1.39.jar

#alias scala=/home/lujin/software/scala-2.11.8/bin/scala
