package cn.wangz.spark.benchmarks

import cn.wangz.spark.benchmarks.BenchmarkType.BenchmarkType
import cn.wangz.spark.benchmarks.tpcds.TPCDSBenchmarkContext
import cn.wangz.spark.benchmarks.tpch.TPCHBenchmarkContext
import org.apache.spark.sql.SparkSession

trait BenchmarkContext {

  def init(): Unit

  def genDataSQLs: Seq[String]

  def querySQLs: Seq[(String, String)]

  def benchmarkType: BenchmarkType

  def genDataCatalog: String = benchmarkType.toString.toLowerCase

  def genDataNamespace(scale: String): String = {
    val database = scale match {
      case "tiny" => "tiny"
      case s => s"sf$s"
    }
    s"$genDataCatalog.$database"
  }

  def checkFilePath: String = s"check_files_${benchmarkType.toString.toLowerCase}"

  def checksumFileName(name: String): String = s"$name.output.hash"

  def spark: SparkSession
}

abstract class BaseBenchmarkContext(sparkSession: SparkSession) extends BenchmarkContext {

  override def spark: SparkSession = sparkSession

  override def genDataSQLs: Seq[String] = {
    val resName = s"${benchmarkType.toString.toLowerCase}/generate.sql"
    val sqlText = Utils.readResource(resName)
    sqlText.split(";").map(_.trim).filter(_.nonEmpty)
  }

  override def querySQLs: Seq[(String, String)] = {
    allQueries().filterNot(excludeQueries().contains(_)).map(name => {
      val query = Utils.readResource(resourceName(name))
      (name, query)
    })
  }

  def resourceName(name: String): String

  def allQueries(): Seq[String]

  def excludeQueries(): Seq[String] = Seq.empty

}

object BenchmarkContext {
    def apply(spark: SparkSession, benchmarkType: String): BenchmarkContext = {
      val context = BenchmarkType.withName(benchmarkType.toUpperCase) match {
        case BenchmarkType.TPCDS => new TPCDSBenchmarkContext(spark)
        case BenchmarkType.TPCH => new TPCHBenchmarkContext(spark)
      }
      context.init()
      context
    }
}

object BenchmarkType extends Enumeration {
  type BenchmarkType = Value
  val TPCDS, TPCH = Value
}
