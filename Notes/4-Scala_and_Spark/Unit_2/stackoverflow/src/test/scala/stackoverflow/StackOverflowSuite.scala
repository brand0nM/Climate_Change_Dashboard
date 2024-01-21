package stackoverflow

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext.*
import org.apache.spark.rdd.RDD
import java.io.File
import scala.io.{ Codec, Source }
import scala.util.Properties.isWin

object StackOverflowSuite:
  val conf: SparkConf = new SparkConf().setMaster("local[2]").setAppName("StackOverflow")
  val sc: SparkContext = new SparkContext(conf)

class StackOverflowSuite extends munit.FunSuite:
  import StackOverflowSuite.*


  lazy val testObject = new StackOverflow {
    override val langs =
      List(
        "JavaScript", "Java", "PHP", "Python", "C#", "C++", "Ruby", "CSS",
        "Objective-C", "Perl", "Scala", "Haskell", "MATLAB", "Clojure", "Groovy")
    override def langSpread = 50000
    override def kmeansKernels = 45
    override def kmeansEta: Double = 20.0D
    override def kmeansMaxIterations = 120
  }

  test("testObject can be instantiated") {
    val instantiatable = try {
      testObject
      true
    } catch {
      case _: Throwable => false
    }
    assert(instantiatable, "Can't instantiate a StackOverflow object")
  }


  import scala.concurrent.duration.given
  override val munitTimeout = 300.seconds


  lazy val lines   = sc.textFile("src/main/resources/stackoverflow/stackoverflow.csv")
  lazy val raw = testObject.rawPostings(lines)

  test("rawPostings"){
    // raw.foreach(println)

    // println(raw.toDebugString)

    // assert(raw.count() == 4000000)
  }

  lazy val grouped = testObject.groupedPostings(raw)

  test("groupedPostings"){
    // grouped.foreach(println)

    // println(grouped.toDebugString)

    // assert(grouped.count() == 20000000)
  }

  lazy val scored  = testObject.scoredPostings(grouped)

  test("scoredPostings"){
    scored.foreach(println)

    // println(scored.toDebugString)

    // assert(scored.count() == 20000000)
    assert(scored.take(2)(1)._2 == 3)
  }

  lazy val vectors = testObject.vectorPostings(scored)

  test("vectorPostings"){
    // vectors.foreach(println)

    // println(vectors.toDebugString)

    // assert(vectors.count() == 20000000)
    assert(vectors.take(2)(0)._1 == 4 * testObject.langSpread)
  }

  lazy val means   = testObject.kmeans(testObject.sampleVectors(vectors), vectors, debug = true)

  // test("kmeans"){
  //   println("K-means: " + means.mkString(" | "))
  //   assert(means(0) == (1,0))
  // }

  lazy val results = testObject.clusterResults(means, vectors)

  // test("results"){
  //   results(0)
  //   testObject.printResults(results)
  //   assert(results(0) ==  ("Java", 100, 1361, 0))
  // } 