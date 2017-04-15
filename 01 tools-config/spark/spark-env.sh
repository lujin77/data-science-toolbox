export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_111.jdk/Contents/Home
export SCALA_HOME=/usr/local/Cellar/scala/2.11.8/libexec


export SPARK_MASTER_IP=localhost
export SPARK_MASTER_PORT=7077
export SPARK_MASTER_WEBUI_PORT=8099

export SPARK_EXECUTOR_MEMORY=1g
export SPARK_DRIVER_MEMORY=1g
export SPARK_WORKER_CORES=2
export SPARK_WORKER_MEMORY=1G
#export SPARK_HOME=/Users/lujin/sofeware/spark-2.0.0-bin-hadoop2.7
#export HADOOP_HOME=/Users/lujin/sofeware/hadoop-2.7.3
export HADOOP_CONF_DIR=$HADOOP_HOME/etc/hadoop
export YARN_CONF_DIR=$HADOOP_HOME/etc/hadoop
#export HIVE_HOME=/opt/apache-hive-1.2.1-bin
export SPARK_PID_DIR=/Users/lujin/tmp/spark/pids
export SPARK_LOG_DIR=/Users/lujin/tmp/spark/logs
export SPARK_WORKER_DIR=/Users/lujin/tmp/spark/worker
#export SPARK_CLASSPATH=$SPARK_CLASSPATH:$SPARK_HOME/lib:$SPARK_HOME/lib/mysql-connector-java-5.1.39.jar
#export SPARK_LIBARY_PATH=$SPARK_LIBARY_PATH:$JAVA_HOME/lib:$HADOOP_HOME/lib/native/
export PYSPARK_PYTHON=$PYTHON_HOME/bin/python
export PYSPARK_DRIVER_PYTHON=$PYTHON_HOME/bin/python


#for libjar in 'ls $SPARK_HOME/lib/*.jar'
#do
#        SPARK_CLASSPATH=$SPARK_CLASSPATH:$libjar
#done
#export SPARK_CLASSPATH
