package exmaple

import java.util.concurrent.Executors

import org.apache.spark.SparkConf
import org.apache.spark.streaming.{Seconds, StreamingContext, TestInputStream}

/**
  * Created by allwefantasy on 11/2/2017.
  */
object JobTest {

  def main(args: Array[String]): Unit = {
    val conf = new SparkConf()
    conf.setAppName("test")
    conf.setMaster("local[4]")
    conf.set("spark.streaming.concurrentJobs", "2")
    conf.set("spark.scheduler.mode", "FIFO")
    val sc = new StreamingContext(conf, Seconds(30))


    val input = new TestInputStream[String](sc, Seq(Seq("1", "2", "3"), Seq("1", "2", "3"), Seq("1", "2", "3")), 1)
    val input2 = new TestInputStream[String](sc, Seq(Seq("1", "2", "3"), Seq("1", "2", "3"), Seq("1", "2", "3")), 1)

    input.map { f =>
      Thread.sleep(5000)
      f
    }

    input2.map { f =>
      Thread.sleep(5000)
      f
    }

    input.union(input2).foreachRDD(rdd=>
      rdd.count()
    )

    input2.foreachRDD(f=> f.count())

    sc.start()
    sc.awaitTermination()

  }
}
