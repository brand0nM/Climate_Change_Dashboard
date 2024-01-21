package reductions

import scala.annotation.*
import org.scalameter.*

object ParallelParenthesesBalancingRunner:

  @volatile var seqResult = false

  @volatile var parResult = false

  val standardConfig = config(
    Key.exec.minWarmupRuns := 40,
    Key.exec.maxWarmupRuns := 80,
    Key.exec.benchRuns := 120,
    Key.verbose := false
  ) withWarmer(Warmer.Default())

  def main(args: Array[String]): Unit =
    val length = 100000000
    val chars = new Array[Char](length)
    val threshold = 10000
    val seqtime = standardConfig measure {
      seqResult = ParallelParenthesesBalancing.balance(chars)
    }
    println(s"sequential result = $seqResult")
    println(s"sequential balancing time: $seqtime")

    val fjtime = standardConfig measure {
      parResult = ParallelParenthesesBalancing.parBalance(chars, threshold)
    }
    println(s"parallel result = $parResult")
    println(s"parallel balancing time: $fjtime")
    println(s"speedup: ${seqtime.value / fjtime.value}")

object ParallelParenthesesBalancing extends ParallelParenthesesBalancingInterface:

  // Returns `true` iff the parentheses in the input `chars` are balanced
  def balance(chars: Array[Char]): Boolean =
    parBalance(chars, 0)

  // Returns `true` iff the parentheses in the input `chars` are balanced
  def parBalance(chars: Array[Char], threshold: Int): Boolean =

    // arg1 is used to balanced parentheses
    // When the segment is not balanced, we use arg2 to accumulate right parenthesis 
    def traverse(idx: Int, until: Int, arg1: Int, arg2: Int): (Int, Int) =
      if idx < until then
        chars(idx) match {
          // In this case we add 1 level to the arg1 acc
          case '(' => traverse(idx + 1, until, arg1 + 1, arg2)
          case ')' => 
            // If this balances arg1, we will decrease its level
            if (arg1 > 0) traverse(idx +1, until, arg1 - 1, arg2)

            // Else we will accumulate right parenthesis in arg2
            else traverse(idx + 1, until, arg1, arg2 + 1)

          // None parenthesis char
          case _ => traverse(idx + 1, until, arg1, arg2)
        }

      // Base of recursion, return 2 accumulators
      else (arg1, arg2)

    // This method recursively splits a segment in parallel 
    def reduce(from: Int, until: Int): (Int, Int) =
      val size = from - until

      // When its length is greater than some threshold
      if size > threshold then
        val mid = size/2
        val ((a1,a2),(b1,b2)) = parallel(reduce(from, mid), reduce(mid+1, until))

        // Tells how to reconstruct Segments
        // When the left segment has more opening parenthesis
        if (a1 > b1)
          // Close halves, from right opening
          (a1-b2+b1) -> a2

        else
          // Close halves, from left closing 
          b1 -> (b2-a1+a2)

      // Otherwise we traverse each deconstructed segment
      else
        traverse(from, until, 0, 0)

    reduce(0, chars.length) == (0, 0) 