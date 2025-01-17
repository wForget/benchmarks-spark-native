package cn.wangz.spark.benchmarks.tpch

import cn.wangz.spark.benchmarks.{BaseBenchmarkContext, BenchmarkType, Utils}
import cn.wangz.spark.benchmarks.BenchmarkType.BenchmarkType
import org.apache.spark.sql.SparkSession

class TPCHBenchmarkContext(spark: SparkSession) extends BaseBenchmarkContext(spark) {

  override def init(): Unit = {
    // configure kyuubi tpcds catalog
    spark.sql("set spark.sql.catalog.tpch=org.apache.kyuubi.spark.connector.tpch.TPCHCatalog")
  }

  override def benchmarkType: BenchmarkType = BenchmarkType.TPCH

  override def resourceName(name: String): String = s"kyuubi/tpch/$name.sql"

  override def allQueries(): Seq[String] = (1 to 22).map(i => s"q$i").toList
}
