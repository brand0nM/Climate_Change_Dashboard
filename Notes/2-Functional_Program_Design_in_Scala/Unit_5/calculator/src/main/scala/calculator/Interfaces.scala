package calculator

/** First Use case of Signal, allows the tweetText and remainingCharacters states to
  * be linked. We can fix a maxmium amount of characters and color based on remaining
  * while keeping the text up to date. */
trait TweetLengthInterface:
  def MaxTweetLength: Int
  def tweetRemainingCharsCount(tweetText: Signal[String]): Signal[Int]
  def colorForRemainingCharsCount(remainingCharsCount: Signal[Int]): Signal[String]

/** This Signal is used to keep the roots of a Polynomial up to-date. As we modify
  * a, b and c, the object will dynamically compute its roots. */
trait PolynomialInterface:
  def computeDelta(a: Signal[Double], b: Signal[Double],
      c: Signal[Double]): Signal[Double] 
  def computeSolutions(a: Signal[Double], b: Signal[Double],
      c: Signal[Double], delta: Signal[Double]): Signal[Set[Double]]

/** In this calculator application, we can dynamically evaluate an expression as new
  * members are added and existing ones are modified or deleted. */
trait CalculatorInterface:
  def computeValues(namedExpressions: Map[String, Signal[Expr]]): Map[String, Signal[Double]]
  def eval(expr: Expr, references: Map[String, Signal[Expr]])(using Signal.Caller): Double

