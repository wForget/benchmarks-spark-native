package cn.wangz.spark.benchmarks

import cn.wangz.spark.benchmarks.benchmark.Benchmark
import org.apache.spark.internal.Logging
import org.apache.spark.sql.{Row, SparkSession}
import org.rogach.scallop.Subcommand

import java.nio.file.{Files, Paths}
import scala.concurrent.duration.DurationInt
import scala.jdk.CollectionConverters._

class BenchmarkRun(context: BenchmarkContext, conf: BenchmarkConf) extends Logging {

  def run(): Unit = {
    init()

    val benchmarkName = conf.name.getOrElse(s"${context.benchmarkType} ${conf.scale()}")

    val benchmark = new Benchmark(benchmarkName, minNumIters = conf.minNumIters(), warmupTime = 200.millis)

    context.querySQLs.foreach { case (name, query) =>
      benchmark.addTimerCase(name) { timer =>
        logInfo(s"Running benchmark case: $name")

        // set query to description
        spark.sparkContext.setJobGroup(s"$name-${timer.iteration}", query)

        timer.startTiming()
        val rows = spark.sql(query).collect()
        timer.stopTiming()

        // check result do not need to be timed
        if (conf.checkResult()) {
          checkResult(name, rows)
        }
      }
    }
    val results = benchmark.run()

    // generate benchmark result
    val resultMap = results.zip(benchmark.benchmarks).map { case (result, benchmarkCase) =>
      val name = benchmarkCase.name
      val time = result.avgMs
      (name, time)
    }.toMap

    val outputFileName = conf.output.getOrElse(s"${context.benchmarkType}_${conf.scale()}.json")
    saveResultJsonFile(outputFileName, Utils.MAPPER.writeValueAsString(resultMap))
  }

  private def init(): Unit = {
    // use target database
    spark.sql(s"use ${conf.database()}")
  }

  private def checkResult(name: String, rows: Array[Row]): Unit = {
    val checksum = if (rows.nonEmpty) {
      val schema = rows.head.schema
      val tempViewName = s"temp_view_${name.replace('.', '_')}"
      spark.createDataFrame(rows.toList.asJava, schema).createTempView(tempViewName)
      spark.sql(s"select sum(hash(*)) from $tempViewName").collect().head.getLong(0)
    } else {
      0
    }

    assert(checksum == readChecksum(name), s"Checksum mismatch for $name")
  }

  private def readChecksum(name: String): Long = {
    val fileName = context.checksumFileName(name)
    val path = Paths.get(context.checkFilePath, fileName)
    Files.readAllLines(path).asScala.head.toLong
  }

  private def saveResultJsonFile(fileName: String, content: String): Unit = {
    val path = Paths.get(fileName)
    Files.write(path, content.getBytes)
  }

  private def spark: SparkSession = context.spark
}

class BenchmarkConf extends Subcommand("benchmark") {
  val `type` = opt[String](required = true, descr = "Benchmark type: tpcds, tpch")
  val scale = opt[String](required = true, descr = "Scale factor: tiny, 1, 10, 100...")
  val name = opt[String](required = false, descr = "The spark benchmark runner name of spark job")
  val output = opt[String](required = false, descr = "The output file name of benchmark result")
  val database = opt[String](required = false, default = Some("default"), descr = "Database name of generated data, default is 'default'")
  val checkResult = opt[Boolean](required = false, default = Some(false), descr = "If check result (hash sum), default is false")
  val minNumIters = opt[Int](required = false, default = Some(3), descr = "Minimum number of iterations to run, default is 3")
}