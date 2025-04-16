package cn.wangz.spark.benchmarks

import org.apache.spark.sql.SparkSession
import org.rogach.scallop.ScallopConf

import scala.sys.exit

object Main {

  def main(args: Array[String]): Unit = {
    val spark = SparkSession
      .builder()
      .enableHiveSupport()
      .getOrCreate()

    new Conf(args).subcommand match {
      case Some(conf: DataGenConf) =>
        val context: BenchmarkContext = BenchmarkContext(spark, conf.`type`())
        new DataGen(context, conf).run()
      case Some(conf: BenchmarkConf) =>
        val context = BenchmarkContext(spark, conf.`type`())
        new BenchmarkRun(context, conf).run()
      case _ =>
        println("Invalid subcommand")
        exit(-1)
    }
  }

  class Conf(arguments: Seq[String]) extends ScallopConf(arguments) {

    object dataGen extends DataGenConf
    addSubcommand(dataGen)

    object benchmark extends BenchmarkConf
    addSubcommand(benchmark)

    verify()
  }

}
