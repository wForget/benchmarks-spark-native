package cn.wangz.spark.benchmarks.tpcds

import cn.wangz.spark.benchmarks.{BaseBenchmarkContext, BenchmarkType, Utils}
import cn.wangz.spark.benchmarks.BenchmarkType.BenchmarkType
import org.apache.spark.sql.SparkSession

class TPCDSBenchmarkContext(spark: SparkSession) extends BaseBenchmarkContext(spark) {

  override def init(): Unit = {
    // configure kyuubi tpcds catalog
    spark.sql("set spark.sql.catalog.tpcds=org.apache.kyuubi.spark.connector.tpcds.TPCDSCatalog")
  }

  override def benchmarkType: BenchmarkType = BenchmarkType.TPCDS

  override def resourceName(name: String): String = s"kyuubi/tpcds_3.2/$name.sql"

  override def allQueries(): Seq[String] = (1 to 99).map(i => s"q$i")
    .filterNot(Seq("q14", "q23", "q24", "q39").contains(_)) ++
    Seq("q14a", "q14b", "q23a", "q23b", "q24a", "q24b", "q39a", "q39b")
}
