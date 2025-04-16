# Benchmarks of spark native engine

## Overview

This is a benchmark framework for Apache Spark, I use it to benchmark spark native engines.

## Generate data

This generates benchmark datasets into spark warehouse by integrating [Apache Kyuubi TPC-DS/TPCH spark connector](https://kyuubi.readthedocs.io/en/master/connector/spark/tpcds.html). We can use it to generate datasets in different formats locally or remotely.

Usage:

```
Main class: cn.wangz.spark.benchmarks.Main

Subcommand: data
  -c, --check-file-gen    If generate checksum file
  -d, --database  <arg>   Database name of generated data
  -f, --format  <arg>     Data format of generated data: parquet, orc, csv...
  -s, --scale  <arg>      Scale factor: tiny, 1, 10, 100...
  -t, --type  <arg>       Benchmark type: tpcds, tpch
  -h, --help              Show help message
```

An example of generating a local dataset:

```
WAREHOUSE_PATH=/path/to/data/warehouse
METASTORE_PATH=/path/to/data/metastore
$SPARK_HOME/bin/spark-submit \
  --master local[*] \
  --conf spark.sql.warehouse.dir=$WAREHOUSE_PATH \
  --conf spark.sql.catalogImplementation=hive \
  --conf spark.hadoop.javax.jdo.option.ConnectionURL=jdbc:derby:\;databaseName=$METASTORE_PATH\;create=true \
  --class cn.wangz.spark.benchmarks.Main \
  benchmarks-spark-native-1.0-SNAPSHOT.jar \
  data --type=tpcds --scale=1
```

## Run queries

Run benchmark queries on spark.

Usage:

```
Main class: cn.wangz.spark.benchmarks.Main

Subcommand: benchmark
  -c, --check-result           If check result (hash sum), default is false
  -d, --database  <arg>        Database name of generated data, default is 'default'
  -m, --min-num-iters  <arg>   Minimum number of iterations to run, default is 3
  -o, --output  <arg>          The output file name of benchmark result
  -s, --scale  <arg>           Scale factor: tiny, 1, 10, 100...
  -t, --type  <arg>            Benchmark type: tpcds, tpch
  -h, --help                   Show help message
```

Example of running spark vanilla and comet benchmark:

```
# Run vanilla spark benchmark
WAREHOUSE_PATH=/path/to/data/warehouse
METASTORE_PATH=/path/to/data/metastore
$SPARK_HOME/bin/spark-submit \
  --master local[4] \
  --conf spark.sql.warehouse.dir=$WAREHOUSE_PATH \
  --conf spark.hadoop.javax.jdo.option.ConnectionURL=jdbc:derby:\;databaseName=$METASTORE_PATH\;create=true \
  --conf spark.driver.memory=4g \
  --class cn.wangz.spark.benchmarks.Main \
  benchmarks-spark-native-1.0-SNAPSHOT.jar \
  benchmark --type=tpcds --scale=1 --output=vanilla-tpcds-1.json

# Run spark benchmark with comet
cp comet-spark-spark${{ spark-binary-version }}_${{ scala-binary-version }}-*.jar $SPARK_HOME/jars
WAREHOUSE_PATH=/path/to/data/warehouse
METASTORE_PATH=/path/to/data/metastore
$SPARK_HOME/bin/spark-submit \
  --master local[4] \
  --conf spark.sql.warehouse.dir=$WAREHOUSE_PATH \
  --conf spark.hadoop.javax.jdo.option.ConnectionURL=jdbc:derby:\;databaseName=$METASTORE_PATH\;create=true \
  --conf spark.driver.memory=4g \
  --conf spark.plugins=org.apache.spark.CometPlugin \
  --conf spark.comet.enabled=true \
  --conf spark.shuffle.manager=org.apache.spark.sql.comet.execution.shuffle.CometShuffleManager \
  --conf spark.memory.offHeap.enabled=true \
  --conf spark.memory.offHeap.size=8g \
  --conf spark.comet.columnar.shuffle.unifiedMemoryAllocatorTest=true \
  --class cn.wangz.spark.benchmarks.Main \
  benchmarks-spark-native-1.0-SNAPSHOT.jar \
  benchmark --type=tpcds --scale=1 --output=comet-tpcds-1.json
```

## Compare results

The comparison script is used to compare multiple benchmark result files. It is mostly copied from [datafusion-benchmarks](https://github.com/apache/datafusion-benchmarks/blob/main/scripts/generate-comparison.py).

Example of comparing two benchmark result files:

```
python scripts/generate-comparison.py \
  vanilla-tpcds-1.json comet-tpcds-1.json \
  --labels "Spark" "Comet" -benchmark "tpcds" --title "tpcds-1"
```