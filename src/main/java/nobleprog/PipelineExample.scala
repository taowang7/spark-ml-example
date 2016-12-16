package nobleprog

import org.apache.spark.{SparkContext, SparkConf}
import org.apache.spark.ml.{Pipeline, PipelineModel}
import org.apache.spark.ml.classification.LogisticRegression
import org.apache.spark.ml.feature.{HashingTF, Tokenizer}
import org.apache.spark.mllib.linalg.Vector
import org.apache.spark.sql.{SQLContext, Row}
/**
 * 16/12/16 WilliamZhu(allwefantasy@gmail.com)
 */
object PipelineExample {
  def main(args: Array[String]) = {
    val conf = new SparkConf().setAppName("PipelineExample")
    conf.setMaster("local[2]")
    val spark  = new SparkContext(conf)
    val sqlContext = SQLContext.getOrCreate(spark)
    // Prepare training documents from a list of (id, text, label) tuples.
    val training = sqlContext.createDataFrame(Seq(
      (0L, "a b c d e spark", 1.0),
      (1L, "b d", 0.0),
      (2L, "spark f g h", 1.0),
      (3L, "hadoop mapreduce", 0.0)
    )).toDF("id", "text", "label")

    // Configure an ML pipeline, which consists of three stages: tokenizer, hashingTF, and lr.
    val tokenizer = new Tokenizer()
      .setInputCol("text")
      .setOutputCol("words")
    val hashingTF = new HashingTF()
      .setNumFeatures(1000)
      .setInputCol(tokenizer.getOutputCol)
      .setOutputCol("features")
    val lr = new LogisticRegression()
      .setMaxIter(10) //设置一个最大的迭代次数，避免收敛问题
      .setRegParam(0.01)   //还记得这个参数么，正则化系数
    val pipeline = new Pipeline()
      .setStages(Array(tokenizer, hashingTF, lr))

    // Fit the pipeline to training documents.
    val model = pipeline.fit(training)

    // now we can optionally save the fitted pipeline to disk
    model.save("/tmp/spark-logistic-regression-model")

    // we can also save this unfit pipeline to disk
    pipeline.save("/tmp/unfit-lr-model")

    // and load it back in during production
    val sameModel = PipelineModel.load("/tmp/spark-logistic-regression-model")

    // Prepare test documents, which are unlabeled (id, text) tuples.
    val test = sqlContext.createDataFrame(Seq(
      (4L, "spark i j k"),
      (5L, "l m n"),
      (6L, "mapreduce spark"),
      (7L, "apache hadoop")
    )).toDF("id", "text")

    // Make predictions on test documents.
    model.transform(test)
      .select("id", "text", "probability", "prediction")
      .collect()
      .foreach { case Row(id: Long, text: String, prob: Vector, prediction: Double) =>
      println(s"($id, $text) --> prob=$prob, prediction=$prediction")
    }
  }
}
