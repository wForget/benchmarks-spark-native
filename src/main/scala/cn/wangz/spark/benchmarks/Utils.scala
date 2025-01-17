package cn.wangz.spark.benchmarks

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule

import java.io.InputStream
import scala.io.{Codec, Source}

object Utils {

  def getContextOrCurrentClassLoader: ClassLoader = {
    Option(Thread.currentThread().getContextClassLoader).getOrElse(getClass.getClassLoader)
  }

  def readResource(resourceName: String): String = {
    var in: InputStream = null
    try {
      in = getContextOrCurrentClassLoader.getResourceAsStream(resourceName)
      Source.fromInputStream(in)(Codec.UTF8).mkString
    } finally {
      if (in != null) in.close()
    }
  }

  val MAPPER = new ObjectMapper()
    .registerModule(DefaultScalaModule)

}
