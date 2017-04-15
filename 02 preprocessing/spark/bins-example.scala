import org.apache.spark.ml.feature.Bucketizer
import org.apache.spark.sql.functions._

val splits = Array(Double.NegativeInfinity, 5, 10, 20, 30, 60, 120, 240, 360, 600, Double.PositiveInfinity)

val sql = """
select dt, city, service_order_id, start_time, arrival_time, cast((start_time - arrival_time) as double) as wait_secs
from yc_bit.ods_service_order
where  start_time>0 and arrival_time>0
and start_time > arrival_time
and city in ('bj', 'sh', 'gz', 'sz', 'wh', 'cd', 'tj')
and dt >= 20170101
"""
val df = spark.sql(sql)

val bucketizer = new Bucketizer()
  .setInputCol("wait_secs")
  .setOutputCol("bins")
  .setSplits(splits)

// Transform original data into its bucket index.
var bucketedData = bucketizer.transform(df)

println(s"Bucketizer output with ${bucketizer.getSplits.length-1} buckets")

val bucket_map: (Double => Int) = (bin: Double) => {
    val lables = Array(5, 10, 20, 30, 60, 120, 240, 360, 600, 10000)
    val idx = bin.toInt
    if (idx < lables.length) {
        lables(idx)
    } else {
        -1
    }
}

val sqlfunc = udf(bucket_map)

bucketedData = bucketedData.withColumn("label", sqlfunc(col("bins")))
bucketedData.show()

bucketedData.createOrReplaceTempView("temp")

outDF = spark.sql("select dt, city, label, count(*) from temp group by dt, city, label order by city, dt, label")

println("all is done")
