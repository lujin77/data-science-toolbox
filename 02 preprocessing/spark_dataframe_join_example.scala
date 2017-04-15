// partition table version

//spark-shell  --master yarn-client --jars software/spark-2.0.1-bin-hadoop2.7/lib/mysql-connector-java-5.1.39.jar < spark/hive2parquet_partition.scala

import org.apache.spark.sql._
import org.apache.spark.sql.functions._
import java.text.SimpleDateFormat
import java.util.Date
import com.google.gson.Gson
import java.util.{Map => JMap, LinkedHashMap}

val hqlContext = new org.apache.spark.sql.hive.HiveContext(sc)

//########### CONFIG ##############################
var city = "hz"
var is_debug = false
var is_init_table = false

var base_date = 20160900
var proc_days = 1
//########### [END] CONFIG ##############################


//########### step 1 setting of ods table in hive ##############################

// load hive table of [city]_ods_service_order
var ods_order_table = "yc_mds_letv.%s_ods_service_order_parquet".format(city)
var ods_order_where = ""
//var ods_where_statements = "and corporate_id=0 and is_asap=1 and status in (7,8) and predict_origin_amount>0 and expect_end_latitude>0"
//if(is_debug) ods_where_statements = "limit 100"
var ods_order_columns = """
service_order_id,
user_id,
driver_id,
product_type_id,
fixed_product_id,
total_amount,
start_time,
end_time,
expect_start_latitude,
expect_start_longitude,
expect_end_latitude,
expect_end_longitude,
0 as predict_distance,
cast(expect_start_time as bigint),
date_format(cast(expect_start_time as timestamp), 'yyyy-MM-dd HH:mm:ss') as start_time_str,
date_format(cast(expect_start_time as timestamp), 'EEEE') as start_day_of_week_str,
case date_format(cast(expect_start_time as timestamp), 'EEEE')
    WHEN 'Monday' THEN 1
    WHEN 'Tuesday' THEN 2
    WHEN 'Wednesday' THEN 3
    WHEN 'Thursday' THEN 4
    WHEN 'Friday' THEN 5
    WHEN 'Saturday' THEN 6
    WHEN 'Sunday' THEN 7
    ELSE null
END as expect_start_day_of_week,
cast(date_format(cast(expect_start_time as timestamp), 'HH') as int) as expect_start_hour_of_day,
expect_end_time,
-1 as expect_end_day_of_week,
-1 as expect_end_hour_of_day,
cast(car_type_id as int),
case
    WHEN ((flag & 2) = 2 or (flag & 2) is null) THEN 'manual'
    WHEN (flag & 2) != 2 THEN 'system'
    ELSE 'unknow'
END as dispatch_type,
deadhead_distance,
predict_origin_amount,
predict_amount,
status as order_status,
reason_id as cancel_reason_id,
is_auto_dispatch,
is_asap,
flag,
corporate_id,
create_time,
actual_time_length,
time_length,
confirm_time,
arrival_time,
dt
"""

// load hive table of service_order_ext
var ods_order_ext_table = "yc_mds_letv.ods_service_order_ext_parquet"
var ods_order_ext_statements = ""
var ods_order_ext_columns = """
service_order_id as order_id_ext,
cast(get_json_object(estimate_snap, '$.end_latitude') as double) as end_latitude_ext,
cast(get_json_object(estimate_snap, '$.end_longitude') as double) as end_longitude_ext,
cast(get_json_object(estimate_snap, '$.distance') as int) as predict_distance_ext,
cast(get_json_object(estimate_snap, '$.time_length') as int) as predict_time_length,
cast(get_json_object(estimate_snap, '$.estimate_price') as string) as estimate_price_json
"""

// load hive table of [city]_ods_service_order
var ods_dispatch_table = "yc_mds_letv.%s_dispatch_detail_info_parquet".format(city)
// order_ids is a temp table in spark
var ods_dispatch_where = "and service_order_id in (select service_order_id from order_ids)"
//if(is_debug) ods_dispatch_where = "limit 100"
var ods_dispatch_columns = """
    service_order_id,
    cast(dispatch_time as bigint),
    cast(response_time as bigint),
    driver_id,
    cast(distance as int),
    cast(round as int),
    cast(batch as int),
    cast(accept_status as int),
    cast(get_json_object(dispatch_snapshot, '$.car_type_id') as int) as car_type_id,
    cast(get_json_object(dispatch_snapshot, '$.distance_time_length') as int) as distance_time_length,
    cast(is_assigned as int),
    cast(get_json_object(add_price_set, '$.total_add_price_rate') as float) as driver_add_rate,
    cast(get_json_object(add_price_set, '$.add_price_rate') as float) as yd_add_price_rate,
    (cast(get_json_object(add_price_set, '$.total_add_price_rate') as float) - cast(get_json_object(add_price_set, '$.add_price_rate') as float)) as customer_add_rate,
    cast(get_json_object(add_price_set, '$.add_total_amount') as int) as driver_add_price,
    cast(get_json_object(add_price_set, '$.bidding_price') as int) as customer_add_price,
    cast(get_json_object(add_price_set, '$.add_price_type') as int) as add_price_type,
    cast(get_json_object(add_price_set, '$.strategy_id') as int) as strategy_id,
    cast(get_json_object(add_price_set, '$.add_price_redispatch') as int) as add_price_redispatch,
    cast(get_json_object(add_price_set, '$.total_magnification') as int) as total_magnification,
    cast(get_json_object(add_price_set, '$.add_price_vip') as int) as add_price_vip,
    dt
"""

//########### step 2 setting of creating joined table ##############################

// create partition table of hive to store bidding feature
var output_table = "%s_bidding_partition".format(city)
hqlContext.sql("use tmp")
if (is_debug) {
    output_table = "%s_bidding_partition_debug".format(city)
    hqlContext.sql("DROP TABLE IF EXISTS tmp.%s".format(output_table))
    println("[INFO] debug mode, delet %s and re-create it".format(output_table))
}

if (!is_debug && is_init_table) {
    hqlContext.sql("DROP TABLE IF EXISTS tmp.%s".format(output_table))
    hqlContext.sql("""
CREATE TABLE IF NOT EXISTS tmp.%s(
  `service_order_id` bigint,
  `user_id` bigint,
  `driver_id` bigint,
  `product_type_id` int,
  `fixed_product_id` int,
  `total_amount` float,
  `start_time` int,
  `end_time` int,
  `expect_start_latitude` double,
  `expect_start_longitude` double,
  `expect_end_latitude` double,
  `expect_end_longitude` double,
  `expect_start_time` bigint,
  `expect_start_day_of_week` int,
  `expect_start_hour_of_day` int,
  `expect_end_time` bigint,
  `expect_end_day_of_week` int,
  `expect_end_hour_of_day` int,
  `predict_distance` int,
  `car_type_id` int,
  `dispatch_type` string,
  `deadhead_distance` double,
  `predict_origin_amount` int,
  `predict_amount` int,
  `order_status` tinyint,
  `cancel_reason_id` int,
  `corporate_id` bigint,
  `is_asap` int,
  `is_auto_dispatch` int,
  `first_dispatch_time` bigint,
  `last_dispatch_time` bigint,
  `init_round` int,
  `init_min_add_rate` float,
  `init_min_bidding_rate` float,
  `init_max_add_rate` float,
  `init_max_bidding_rate` float,
  `sum_valid_driver` bigint,
  `mean_driver_distance` double,
  `first_response_round` int,
  `first_response_time` bigint,
  `first_response_min_add_rate` float,
  `first_response_min_bidding_rate` float,
  `first_response_max_add_rate` float,
  `first_response_max_bidding_rate` float,
  `max_no_accept_round` int,
  `max_no_accept_max_add_rate` float,
  `total_round` int,
  `final_add_rate` float,
  `final_bidding_rate` float,
  `total_batch` int,
  `init_batch` int,
  `first_response_batch` int,
  `max_no_accept_batch` int,
  `first_bidding_round` int,
  `frist_bidding_batch` int,
  `frist_bidding_min_rate` float,
  `sum_accept_driver` int,
  `mean_accept_driver_distance` float,
  `create_time` int,
  `actual_time_length` int,
  `time_length` int,
  `confirm_time` int,
  `arrival_time` int
) PARTITIONED BY  (`dt` int)
STORED AS PARQUET
    """.format(output_table))

    println("[INFO] init in proc mode, delet %s and re-create it".format(output_table))
}


//########### [END]  step 2 setting of creating joined table  ##############################


// get predict price from json map
val getPrice: ((String, Int) => Int) = (json: String, id: Int) => {

    var price: Int = 0

    if (json != null && json != "") {
        var gson = new Gson()
        try {
            var result:Double = gson.fromJson(json, (new LinkedHashMap[String, Double]()).getClass).get(id.toString)
            if (result != null) {
                price = result.intValue
            }
        } catch {
            case e:Exception => price = 0
        }
    }

    price
}
val sqlfunc = udf(getPrice)

def getNowDate():String={
    var now:Date = new Date()
    var dateFormat:SimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    var timeStr = dateFormat.format( now )
    timeStr
  }



//########### step 3 join tables ##############################

// debug mode only use one day's data
if(is_debug) proc_days = 1

// loop for each day's partition
for(i <- 1 to proc_days) {
    // set current working date
    var now_date = base_date + i

    hqlContext.sql("use yc_mds_letv")
    // load ods_service_order data from hive
    ods_order_where = "and service_order_id in (select service_order_id from %s where dt=%d)".format(ods_dispatch_table, now_date)
    var ods_order_sql = "select %s from %s where dt=%d %s".format(ods_order_columns, ods_order_table, now_date, ods_order_where)
    if (is_debug) {
        println("[DEBUG] load service_order partition[%d], SQL -> %s".format(now_date, ods_order_sql))
    }
    var orderDF =  hqlContext.sql(ods_order_sql)
    orderDF.select("service_order_id").createOrReplaceTempView("order_ids")
    println("[TRACE] load ods_service_order partition[%d]".format(now_date))
    if (is_debug) {
        orderDF.printSchema
        println("[DEBUG] load service_order partition[%d], total -> %d".format(now_date, orderDF.count))
    }

    // load ods_service_order_ext data from hive
    var ods_order_ext_sql = "select %s from %s where dt=%d".format(ods_order_ext_columns, ods_order_ext_table, now_date)
    var orderExtDF =  hqlContext.sql(ods_order_ext_sql).dropDuplicates("order_id_ext")

    // case 1. data load >= 20160925 that set expect_end_time=0, other set to null
    orderDF = orderDF.join(orderExtDF, orderDF("service_order_id") <=> orderExtDF("order_id_ext"), "left")
                .drop("order_id_ext")
                .withColumn("predict_distance", $"predict_distance_ext")
                .drop("predict_distance_ext")
                .withColumn("expect_end_latitude", when($"expect_end_latitude".isNull, $"end_latitude_ext").otherwise($"expect_end_latitude"))
                .withColumn("expect_end_longitude", when($"expect_end_longitude".isNull, $"end_longitude_ext").otherwise($"expect_end_longitude"))
                .drop("end_latitude_ext")
                .drop("end_longitude_ext")
                .withColumn("expect_end_time",
                    expr("""CASE
                                WHEN expect_end_time is null or expect_end_time=0 then null
                                ELSE expect_end_time
                            END"""))
                .withColumn("expect_end_time",
                    expr("""CASE
                                WHEN expect_end_time is null and predict_time_length>0 then expect_start_time + predict_time_length
                                ELSE expect_end_time
                            END"""))
                .withColumn("expect_end_time_str",
                    expr("""CASE
                                WHEN expect_end_time is null then null
                                ELSE date_format(cast(expect_end_time as timestamp), 'yyyy-MM-dd HH:mm:ss')
                            END"""))
                .drop("predict_time_length")
                .withColumn("expect_end_day_of_week",
                    expr("""CASE
                                WHEN expect_end_time is not null and date_format(cast(expect_end_time as timestamp), 'EEEE')='Monday' THEN 1
                                WHEN expect_end_time is not null and date_format(cast(expect_end_time as timestamp), 'EEEE')='Tuesday' THEN 2
                                WHEN expect_end_time is not null and date_format(cast(expect_end_time as timestamp), 'EEEE')='Wednesday' THEN 3
                                WHEN expect_end_time is not null and date_format(cast(expect_end_time as timestamp), 'EEEE')='Thursday' THEN 4
                                WHEN expect_end_time is not null and date_format(cast(expect_end_time as timestamp), 'EEEE')='Friday' THEN 5
                                WHEN expect_end_time is not null and date_format(cast(expect_end_time as timestamp), 'EEEE')='Saturday' THEN 6
                                WHEN expect_end_time is not null and date_format(cast(expect_end_time as timestamp), 'EEEE')='Sunday' THEN 7
                                ELSE null
                            END"""))
                .withColumn("expect_end_hour_of_day",
                    expr("""CASE
                                WHEN expect_end_time is not null THEN cast(date_format(cast(expect_end_time as timestamp), 'HH') as int)
                                ELSE null
                            END"""))
                .withColumn("is_auto_dispatch",
                                        expr("""case
                                                    when flag & 34359738368 = 34359738368 then 1
                                                    else 0
                                                end"""))

    // load data from dispatch_detail
    var ods_dispatch_sql = "select %s from %s where dt=%d %s".format(ods_dispatch_columns, ods_dispatch_table, now_date, ods_dispatch_where)
    if (is_debug) {
        println("[DEBUG] load ods_dispatch_detail_info partition[%d], SQL -> %s".format(now_date, ods_dispatch_sql))
    }
    var dispatchDF =  hqlContext.sql(ods_dispatch_sql)
    println("[TRACE] load ods_dispatch_detail_info partition[%d]".format(now_date))
    if (is_debug) {
        dispatchDF.printSchema
        println("[DEBUG] load ods_dispatch_detail_info partition[%d], total -> %d".format(now_date, dispatchDF.count))
    }
    spark.catalog.dropTempView("order_ids")

    //#######  aggregation for dispatch data  ###############

    // setp 0. cumulative batch
    // key => fid_cum_batch (order),  round_cum_batch (round tag)
    // value => cum_batch
    var cum_batch_base = dispatchDF
                            .select("service_order_id", "round", "batch")
                            .sort($"service_order_id", $"round".desc, $"batch".desc)
                            .dropDuplicates(Seq("service_order_id", "round"))
    cum_batch_base.createOrReplaceTempView("tmp_cum_batch_base")

    cum_batch_base = sqlContext.sql("select service_order_id as fid_cum_batch, round as round_cum_batch,sum(batch) over (partition by service_order_id order by round asc) as cum_batch from tmp_cum_batch_base")

    println("[TRACE] cum_batch_base -> get cumulative batch of each round")


    //////////////////////////////////////////////////////////////////////////////////////////


    // setp 1. group dispatch_detail by order_id
    var agg_order = dispatchDF
                        .select("service_order_id", "dispatch_time", "round", "driver_add_rate", "customer_add_rate")
                        .groupBy($"service_order_id".alias("fid_agg_order"))
                        .agg(max("round").alias("total_round"),
                                min("round").alias("init_round"),
                                min("dispatch_time").alias("first_dispatch_time"),
                                max("dispatch_time").alias("last_dispatch_time"),
                                max("driver_add_rate").alias("final_add_rate"),
                                max("customer_add_rate").alias("final_bidding_rate"))
                        .withColumn("final_add_rate", when($"final_add_rate".isNull, 0).otherwise($"final_add_rate"))
                        .withColumn("final_bidding_rate", when($"final_bidding_rate".isNull, 0).otherwise($"final_bidding_rate"))

    if (is_debug) println("[DEBUG] agg_order -> groupBy order_id")
    // setp 1-1. merge cumulative batch (total)
    agg_order = agg_order
                    .join(cum_batch_base, agg_order("fid_agg_order") <=> cum_batch_base("fid_cum_batch")
                                            && agg_order("total_round") <=> cum_batch_base("round_cum_batch"), "left")
                    .withColumn("total_batch", $"cum_batch")
                    .drop("fid_cum_batch")
                    .drop("round_cum_batch")
                    .drop("cum_batch")

    // setp 1-2. merge cumulative batch (init)
    agg_order = agg_order
                    .withColumn("init_round_minus_1", $"init_round"-1)
                    .join(cum_batch_base, agg_order("fid_agg_order") <=> cum_batch_base("fid_cum_batch")
                                            && agg_order("init_round") <=> cum_batch_base("round_cum_batch"), "left")
                    .withColumn("init_batch",
                                    expr("""case
                                                when init_round_minus_1=0 then 1
                                                else cum_batch + 1
                                            end"""))
                    .drop("fid_cum_batch")
                    .drop("round_cum_batch")
                    .drop("cum_batch")
                    .drop("init_round_minus_1")

    if (is_debug) println("[DEBUG] agg_order -> add cum")

    if (is_debug) println("[DEBUG] init order size=%d".format(orderDF.count))
    // [NOTICE] merge agg_order, add detail:
    // total_round
    // init_round               (include not from rond 1)
    // first_dispatch_time      (default)
    // last_dispatch_time
    // final_add_rate           (default)
    // final_bidding_rate       (default)
    // total_batch
    // init_batch               (default)
    var joined_orderDF = orderDF
                            .join(agg_order, orderDF("service_order_id") <=> agg_order("fid_agg_order"), "left")
                            .drop("fid_agg_order")
    println("[TRACE] merge agg_order into orderDF")
    if (is_debug) println("[DEBUG] merge agg_order -> order size=%d".format(joined_orderDF.count))


    //////////////////////////////////////////////////////////////////////////////////////////


    // setp 2-1. join by order_id and min_round
    var init_round_default = agg_order
                                .join(dispatchDF, agg_order("fid_agg_order") <=> dispatchDF("service_order_id")
                                                    && agg_order("init_round") <=> dispatchDF("round"), "left")
                                .groupBy($"fid_agg_order".alias("fid_init_round_default"))
                                .agg(min("driver_add_rate").alias("init_min_add_rate"),
                                            min("customer_add_rate").alias("init_min_bidding_rate"),
                                            max("driver_add_rate").alias("init_max_add_rate"),
                                            max("customer_add_rate").alias("init_max_bidding_rate"))
                                .withColumn("init_min_add_rate", when($"init_min_add_rate".isNull, 0).otherwise($"init_min_add_rate"))
                                .withColumn("init_min_bidding_rate", when($"init_min_bidding_rate".isNull, 0).otherwise($"init_min_bidding_rate"))
                                .withColumn("init_max_add_rate", when($"init_max_add_rate".isNull, 0).otherwise($"init_max_add_rate"))
                                .withColumn("init_max_bidding_rate", when($"init_max_bidding_rate".isNull, 0).otherwise($"init_max_bidding_rate"))
    if (is_debug) println("[DEBUG] init_round_default -> groupBy order_id and init_round")

    // setp 2-2. get round=1 and batch=1
    var init_round = dispatchDF
                            .filter("round=1 and batch=1")
                            .select("service_order_id", "dispatch_time", "driver_id", "round", "batch", "driver_add_rate", "customer_add_rate")
                            .groupBy($"service_order_id".alias("fid_init_round"))
                            .agg(min("driver_add_rate").alias("init_min_add_rate_1"),
                                    min("customer_add_rate").alias("init_min_bidding_rate_1"),
                                    max("driver_add_rate").alias("init_max_add_rate_1"),
                                    max("customer_add_rate").alias("init_max_bidding_rate_1"))
    if (is_debug) println("[DEBUG] init_round -> round=1 and batch=1")

    // merge init round by min round and batch
    joined_orderDF = joined_orderDF
                            .join(init_round_default, joined_orderDF("service_order_id") <=> init_round_default("fid_init_round_default"), "left")
                            .drop("fid_init_round_default")
    println("[TRACE] merge init_round_default into orderDF")
    if (is_debug) println("[DEBUG] merge init_round_default -> order size=%d".format(joined_orderDF.count))

    // merge and update init round by round=1 and batch=1
    joined_orderDF = joined_orderDF
                            .join(init_round, joined_orderDF("service_order_id") <=> init_round("fid_init_round"), "left")
                            .drop("fid_init_round")
                            .withColumn("init_min_add_rate",
                                            expr("""case
                                                        when init_min_add_rate_1>0 then init_min_add_rate_1
                                                        else init_min_add_rate
                                                    end""")
                                        )
                            .drop("init_min_add_rate_1")
                            .withColumn("init_min_bidding_rate",
                                            expr("""case
                                                        when init_min_bidding_rate_1>0 then init_min_bidding_rate_1
                                                        else init_min_bidding_rate
                                                    end""")
                                        )
                            .drop("init_min_bidding_rate_1")
                            .withColumn("init_max_add_rate",
                                            expr("""case
                                                        when init_max_add_rate_1>0 then init_max_add_rate_1
                                                        else init_max_add_rate
                                                    end""")
                                        )
                            .drop("init_max_add_rate_1")
                            .withColumn("init_max_bidding_rate",
                                            expr("""case
                                                        when init_max_bidding_rate_1>0 then init_max_bidding_rate_1
                                                        else init_max_bidding_rate
                                                    end""")
                                        )
                            .drop("init_max_bidding_rate_1")
    println("[TRACE] update init_round (round=1 and batch=1) into orderDF")
    if (is_debug) println("[DEBUG] merge init_round -> order size=%d".format(joined_orderDF.count))


    //////////////////////////////////////////////////////////////////////////////////////////


    // setp 3-1. get valid driver
    var last_order_driver = dispatchDF
                            .select("service_order_id", "driver_id", "round", "batch", "distance", "driver_add_rate", "customer_add_rate")
                            .sort($"service_order_id", $"driver_id", $"round".desc, $"batch".desc)
                            .dropDuplicates(Seq("service_order_id", "driver_id"))
                            .select($"service_order_id".alias("fid_driver"),
                                            $"driver_id".alias("driver_id_ext"),
                                            $"distance".alias("final_driver_distance"),
                                            $"driver_add_rate".alias("final_add_rate_ext"),
                                            $"customer_add_rate".alias("final_bidding_rate_ext"))
                            .withColumn("final_add_rate_ext", when($"final_add_rate_ext".isNull, 0).otherwise($"final_add_rate_ext"))
                            .withColumn("final_bidding_rate_ext", when($"final_bidding_rate_ext".isNull, 0).otherwise($"final_bidding_rate_ext"))

    // setp 3-2. driver count and mean
    var agg_order_driver = last_order_driver
                                .groupBy($"fid_driver")
                                .agg(count("driver_id_ext").alias("sum_valid_driver"),
                                                mean("final_driver_distance").alias("mean_driver_distance"))
    if (is_debug) println("[DEBUG] last_order_driver and agg_order_driver")

    // merge sum_valid_driver and mean_driver_distance
    joined_orderDF = joined_orderDF
                            .join(agg_order_driver, joined_orderDF("service_order_id") <=> agg_order_driver("fid_driver"), "left")
                            .drop("fid_driver")
    println("[TRACE] merge agg_order_driver into orderDF")
    if (is_debug) println("[DEBUG] merge agg_order_driver -> order size=%d".format(joined_orderDF.count))

    // merge last driver distance
    joined_orderDF = joined_orderDF
                        .join(last_order_driver, joined_orderDF("service_order_id") <=> last_order_driver("fid_driver")
                                                 && joined_orderDF("driver_id") <=> last_order_driver("driver_id_ext"), "left")
                        .withColumn("deadhead_distance",
                                        expr("""case
                                                    when final_driver_distance>0 then final_driver_distance
                                                    when final_driver_distance is null or final_driver_distance=0 then mean_driver_distance
                                                    else deadhead_distance
                                                end"""))
                        .withColumn("final_add_rate", when($"final_add_rate_ext">0, $"final_add_rate_ext").otherwise($"final_add_rate"))
                        .withColumn("final_bidding_rate", when($"final_bidding_rate_ext">0, $"final_bidding_rate_ext").otherwise($"final_bidding_rate"))
                        .drop("fid_driver")
                        .drop("driver_id_ext")
                        .drop("final_add_rate_ext")
                        .drop("final_bidding_rate_ext")
    println("[TRACE] merge last_order_driver into orderDF")
    if (is_debug) println("[DEBUG] merge last_order_driver -> order size=%d".format(joined_orderDF.count))


    //////////////////////////////////////////////////////////////////////////////////////////


    // setp 4-1. first_accept_round
    var first_accept_round = dispatchDF
                                .filter("accept_status=1")
                                .select("service_order_id", "round", "batch", "driver_add_rate", "customer_add_rate")
                                .sort($"service_order_id", $"round", $"batch")
                                .dropDuplicates(Seq("service_order_id"))
                                .select($"service_order_id".alias("fid_first"),
                                            $"round".alias("first_response_round"),
                                            $"batch".alias("first_response_batch"))
                        .withColumn("max_no_accept_round", col("first_response_round")-1)

    // setp 4-2. merge cumulative batch (first_accept_round)
    first_accept_round = first_accept_round
                            .join(cum_batch_base, first_accept_round("fid_first") <=> cum_batch_base("fid_cum_batch")
                                                    && first_accept_round("max_no_accept_round") <=> cum_batch_base("round_cum_batch"), "left")
                            .withColumn("first_response_batch",
                                    expr("""case
                                                when max_no_accept_round=0 then first_response_batch
                                                when max_no_accept_round>0 then (first_response_batch + cum_batch)
                                                else null
                                            end"""))
                            .withColumn("max_no_accept_batch",
                                            expr("""case
                                                        when max_no_accept_round=0 then null
                                                        when first_response_batch>1 then first_response_batch-1
                                                        else null
                                                    end"""))
                            .drop("fid_cum_batch")
                            .drop("round_cum_batch")
                            .drop("cum_batch")

    // setp 4-3. maybe nobady accept
    var first_accept_value = first_accept_round
                                .select("fid_first", "first_response_round")
                                .join(dispatchDF, first_accept_round("fid_first") <=> dispatchDF("service_order_id")
                                                    && first_accept_round("first_response_round") <=> dispatchDF("round"), "left")
                                .filter("accept_status=1")
                                .groupBy($"fid_first".alias("fid_first_ext"))
                                .agg(min("response_time").alias("first_response_time"),
                                        min("driver_add_rate").alias("first_response_min_add_rate"),
                                        min("customer_add_rate").alias("first_response_min_bidding_rate"),
                                        max("driver_add_rate").alias("first_response_max_add_rate"),
                                        max("customer_add_rate").alias("first_response_max_bidding_rate"))
                                .withColumn("first_response_min_add_rate", when($"first_response_min_add_rate".isNull, 0).otherwise($"first_response_min_add_rate"))
                                .withColumn("first_response_min_bidding_rate", when($"first_response_min_bidding_rate".isNull, 0).otherwise($"first_response_min_bidding_rate"))
                                .withColumn("first_response_max_add_rate", when($"first_response_max_add_rate".isNull, 0).otherwise($"first_response_max_add_rate"))
                                .withColumn("first_response_max_bidding_rate", when($"first_response_max_bidding_rate".isNull, 0).otherwise($"first_response_max_bidding_rate"))
    if (is_debug) println("[DEBUG] first_accept_round and first_accept_value")


    // merge sum_valid_driver and mean_driver_distance
    joined_orderDF = joined_orderDF
                        .join(first_accept_round, orderDF("service_order_id") <=> first_accept_round("fid_first"), "left")
                        .withColumn("first_response_round", when($"first_response_round".isNull, 0).otherwise($"first_response_round")) // nobady accept
                        .drop("fid_first")
    println("[TRACE] merge first_accept_round into orderDF")
    if (is_debug) println("[DEBUG] merge first_accept_round -> order size=%d".format(joined_orderDF.count))

    joined_orderDF = joined_orderDF
                        .join(first_accept_value, orderDF("service_order_id") <=> first_accept_value("fid_first_ext"), "left")
                        .drop("fid_first_ext")
    println("[TRACE] merge first_accept_value into orderDF")
    if (is_debug) println("[DEBUG] merge first_accept_value -> order size=%d".format(joined_orderDF.count))


    //////////////////////////////////////////////////////////////////////////////////////////

    // setp 5. max nobady accept, add:
    // max_no_accept_max_add_rate
    var max_no_accept = first_accept_round
                            .select("fid_first", "max_no_accept_round")
                            .join(dispatchDF, first_accept_round("fid_first") <=> dispatchDF("service_order_id")
                                                && first_accept_round("max_no_accept_round") <=> dispatchDF("round"), "left")
                            .select($"fid_first".alias("fid_noaccept"), $"max_no_accept_round", $"driver_add_rate")
                            .sort($"fid_noaccept", $"max_no_accept_round", $"driver_add_rate".desc)
                            .dropDuplicates(Seq("fid_noaccept", "max_no_accept_round"))
                            .withColumn("max_no_accept_max_add_rate",
                                                expr("""CASE
                                                            WHEN max_no_accept_round=0 THEN null
                                                            WHEN max_no_accept_round>0 and driver_add_rate is null THEN 0
                                                            WHEN max_no_accept_round>0 and driver_add_rate is not null THEN driver_add_rate
                                                            ELSE null
                                                        END"""))
                            .drop("max_no_accept_round")
                            .drop("driver_add_rate")
    if (is_debug) println("[DEBUG] max_no_accept")

    // merge max nobady accept
    joined_orderDF = joined_orderDF
                        .join(max_no_accept, orderDF("service_order_id") <=> max_no_accept("fid_noaccept"), "left")
                        .withColumn("max_no_accept_round", when($"max_no_accept_round".isNull, $"total_round").otherwise($"max_no_accept_round")) // same
                        .withColumn("max_no_accept_batch", when($"max_no_accept_batch".isNull, $"total_batch").otherwise($"max_no_accept_batch")) // same
                        .drop("fid_noaccept")
    println("[TRACE] merge max_no_accept into orderDF")
    if (is_debug) println("[DEBUG] merge max_no_accept -> order size=%d".format(joined_orderDF.count))


    //////////////////////////////////////////////////////////////////////////////////////////

    // setp 6. first bidding round, add:
    // first_bidding_round
    // frist_bidding_batch
    // frist_bidding_min_rate
    var first_bidding = dispatchDF
                            .filter("customer_add_rate>0")
                            .select("service_order_id", "round", "batch", "driver_add_rate", "customer_add_rate")
                            .sort($"service_order_id", $"round", $"batch", $"customer_add_rate")
                            .dropDuplicates(Seq("service_order_id"))
                            .select($"service_order_id".alias("fid_first_bidding"),
                                            $"round".alias("first_bidding_round"),
                                            $"batch".alias("frist_bidding_batch"),
                                            $"customer_add_rate".alias("frist_bidding_min_rate"))
                            .withColumn("first_bidding_preround", $"first_bidding_round"-1)
    // setp 6-2. merge cumulative batch (init)
    first_bidding = first_bidding
                        .join(cum_batch_base, first_bidding("fid_first_bidding") <=> cum_batch_base("fid_cum_batch")
                                                    && first_bidding("first_bidding_preround") <=> cum_batch_base("round_cum_batch"), "left")
                        .withColumn("frist_bidding_batch",
                                        expr("""case
                                                    when first_bidding_preround=0 then frist_bidding_batch
                                                    else cum_batch + frist_bidding_batch
                                                end"""))
                        .drop("fid_cum_batch")
                        .drop("round_cum_batch")
                        .drop("cum_batch")
                        .drop("first_bidding_preround")
    if (is_debug) println("[DEBUG] first_bidding")
    // merge max nobady accept
    joined_orderDF = joined_orderDF
                        .join(first_bidding, orderDF("service_order_id") <=> first_bidding("fid_first_bidding"), "left")
                        .withColumn("first_bidding_round", when($"first_bidding_round".isNull, 0).otherwise($"first_bidding_round"))
                        .withColumn("frist_bidding_batch", when($"frist_bidding_batch".isNull, 0).otherwise($"frist_bidding_batch")) // same
                        .drop("fid_first_bidding")
    println("[TRACE] merge first_bidding into orderDF")
    if (is_debug) println("[DEBUG] merge first_bidding -> order size=%d".format(joined_orderDF.count))


    //////////////////////////////////////////////////////////////////////////////////////////

    // setp 7. accept driver, add:
    // sum_accept_driver
    // mean_accept_driver_distance
    var accept_driver = dispatchDF
                            .filter("accept_status=1")
                            .select("service_order_id", "driver_id", "round", "batch", "distance")
                            .sort($"service_order_id", $"driver_id", $"round".desc, $"batch".desc)
                            .dropDuplicates(Seq("service_order_id", "driver_id"))
                            .select($"service_order_id".alias("fid_accept_driver"),
                                            $"driver_id".alias("accept_driver_id"),
                                            $"distance".alias("accept_driver_distance"))
                            .groupBy($"fid_accept_driver")
                            .agg(count("accept_driver_id").alias("sum_accept_driver"),
                                        mean("accept_driver_distance").alias("mean_accept_driver_distance"))
    if (is_debug) println("[DEBUG] accept_driver")
    // merge max nobady accept
    joined_orderDF = joined_orderDF
                        .join(accept_driver, orderDF("service_order_id") <=> accept_driver("fid_accept_driver"), "left")
                        .withColumn("sum_accept_driver", when($"sum_accept_driver".isNull, 0).otherwise($"sum_accept_driver"))
                        .drop("fid_accept_driver")
    println("[TRACE] merge accept_driver into orderDF")
    if (is_debug) println("[DEBUG] merge accept_driver -> order size=%d".format(joined_orderDF.count))


    //////////////////////////////////////////////////////////////////////////////////////////


    // fix merge result last, add:
    // predict_origin_amount
    // predict_amount
    joined_orderDF = joined_orderDF
                        .withColumn("predict_origin_amount", when($"predict_origin_amount" <=> 0, sqlfunc($"estimate_price_json", $"car_type_id")).otherwise($"predict_origin_amount"))
                        .withColumn("predict_amount", when($"predict_amount" <=> 0, sqlfunc($"estimate_price_json", $"car_type_id")).otherwise($"predict_amount"))
                        .drop("estimate_price_json")


    println("[INFO] %s all join aggregation is done".format(getNowDate()))


    //#######  store merged result to hive by partition  ###############

    if (is_debug) {
        var current_cnt = joined_orderDF.count
        println("[TRACE] joined count=%d partition[%d]".format(current_cnt, now_date))
        joined_orderDF.printSchema
    }
    joined_orderDF.createOrReplaceTempView("bidding_feature")

    hqlContext.sql("use tmp")
    if (is_debug) {
        joined_orderDF.write.mode(SaveMode.Overwrite).saveAsTable(output_table)
    } else {
        var insert_hql = """
        INSERT OVERWRITE TABLE %s PARTITION(dt=%d)
        SELECT
        service_order_id,user_id,driver_id,product_type_id,fixed_product_id,total_amount,start_time,end_time,expect_start_latitude,expect_start_longitude,expect_end_latitude,expect_end_longitude,expect_start_time,expect_start_day_of_week,expect_start_hour_of_day,expect_end_time,expect_end_day_of_week,expect_end_hour_of_day,predict_distance,car_type_id,dispatch_type,deadhead_distance,predict_origin_amount,predict_amount,order_status,cancel_reason_id,corporate_id,is_asap,is_auto_dispatch,first_dispatch_time,last_dispatch_time,init_round,init_min_add_rate,init_min_bidding_rate,init_max_add_rate,init_max_bidding_rate,sum_valid_driver,mean_driver_distance,first_response_round,first_response_time,first_response_min_add_rate,first_response_min_bidding_rate,first_response_max_add_rate,first_response_max_bidding_rate,max_no_accept_round,max_no_accept_max_add_rate,total_round,final_add_rate,final_bidding_rate,total_batch,init_batch,first_response_batch,max_no_accept_batch,first_bidding_round,frist_bidding_batch,frist_bidding_min_rate,sum_accept_driver,mean_accept_driver_distance,create_time,actual_time_length,time_length,confirm_time,arrival_time
        FROM bidding_feature where dt=%d
        """.format(output_table, now_date, now_date)
        hqlContext.sql(insert_hql)
    }
    println("[TRACE] %s  write %s partition[%d]".format(getNowDate(), output_table, now_date))
    spark.catalog.dropTempView("bidding_feature")

    //####### [END] store merged result to hive by partition  ###############

    println("[INFO] write bidding feature[dt=%d] to hive success!".format(now_date))


}

//########### [END] step 3 join tables ##############################

println("[NOTICE] all is done!")
