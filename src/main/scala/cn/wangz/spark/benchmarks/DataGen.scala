package cn.wangz.spark.benchmarks

import org.apache.spark.internal.Logging
import org.apache.spark.sql.SparkSession
import org.rogach.scallop.Subcommand

import java.nio.file.{Files, Paths}

class DataGen(context: BenchmarkContext, conf: DataGenConf) extends Logging {

  def run(): Unit = {
    logInfo("Running data generation...")
    init()

    // generate data
    context.genDataSQLs.foreach { sql =>
      logInfo(s"Running data generation sql: $sql")
      spark.sql(sql).collect()
    }

    if (conf.checkFileGen()) {
      genCheckFile()
    }
    logInfo("Data generation finished.")
  }

  private def init(): Unit = {
    // initialize some variables for generate sql
    spark.sql(s"set DATA_GEN_NS=${context.genDataNamespace(conf.scale())}")
    spark.sql(s"set DATA_GEN_FORMAT=${conf.format()}")

    // use target database
    spark.sql(s"create database if not exists ${conf.database()}")
    spark.sql(s"use ${conf.database()}")
  }

  private def genCheckFile(): Unit = {
    val dir = Paths.get(context.checkFilePath)
    if (!Files.exists(dir)) {
      Files.createDirectories(dir)
    }
    context.querySQLs.foreach { case (name, query) =>
      val df = spark.sql(query)
      val tempViewName = s"temp_view_${name.replace('.', '_')}"
      df.createTempView(tempViewName)
      val rows = spark.sql(s"select sum(hash(*)) from $tempViewName").collect()
      val checksum = if (rows.nonEmpty) {
        rows.head.getLong(0)
      } else {
        0L
      }
      saveChecksumFile(name, String.valueOf(checksum))
    }
  }

  private def saveChecksumFile(name: String, content: String): Unit = {
    val fileName = context.checksumFileName(name)
    val path = Paths.get(context.checkFilePath, fileName)
    Files.write(path, content.getBytes)
  }

  private def spark: SparkSession = context.spark

}

class DataGenConf extends Subcommand("data") {
  val `type` = opt[String](required = true, descr = "Benchmark type: tpcds, tpch")
  val scale = opt[String](required = true, descr = "Scale factor: tiny, 1, 10, 100...")
  val format = opt[String](required = false, default = Some("parquet"), descr = "Data format of generated data: parquet, orc, csv...")
  val database = opt[String](required = false, default = Some("default"), descr = "Database name of generated data")
  val checkFileGen = opt[Boolean](required = false, default = Some(false), descr = "If generate checksum file")
}
