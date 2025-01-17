package cn.wangz.spark.benchmarks

import org.apache.spark.sql.SparkSession
import org.rogach.scallop.Subcommand

import java.nio.file.{Files, Paths}

class DataGen(context: BenchmarkContext, conf: DataGenConf) {

  def run(): Unit = {
    init()

    // generate data
    context.genDataSQLs.foreach { sql =>
      spark.sql(sql).collect()
    }

    if (conf.checkFileGen()) {
      genCheckFile()
    }
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
    context.querySQLs.foreach { case (name, query) =>
      val df = spark.sql(query)
      val tempViewName = s"temp_view_${name.replace('.', '_')}"
      df.createTempView(tempViewName)
      val checksumText = spark.sql(s"select sum(hash(*)) from $tempViewName").collect().head.get(0) + "\n"
      saveChecksumFile(name, checksumText)
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
  val `type` = opt[String](required = true)
  val scale = opt[String](required = true)
  val format = opt[String](required = false, default = Some("parquet"))
  val database = opt[String](required = false, default = Some("default"))
  val checkFileGen = opt[Boolean](required = false, default = Some(false))
}
