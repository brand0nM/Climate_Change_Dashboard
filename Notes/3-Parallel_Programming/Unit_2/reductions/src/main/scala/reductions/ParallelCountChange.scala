package reductions

import org.scalameter.*

object ParallelCountChangeRunner:

  @volatile var seqResult = 0

  @volatile var parResult = 0

  val standardConfig = config(
    Key.exec.minWarmupRuns := 20,
    Key.exec.maxWarmupRuns := 40,
    Key.exec.benchRuns := 80,
    Key.verbose := false
  ) withWarmer(Warmer.Default())

  def main(args: Array[String]): Unit =
    val amount = 250
    val coins = List(1, 2, 5, 10, 20, 50)
    val seqtime = standardConfig measure {
      seqResult = ParallelCountChange.countChange(amount, coins)
    }
    println(s"sequential result = $seqResult")
    println(s"sequential count time: $seqtime")

    def measureParallelCountChange(threshold: => ParallelCountChange.Threshold): Unit = try
      val fjtime = standardConfig measure {
        parResult = ParallelCountChange.parCountChange(amount, coins, threshold)
      }
      println(s"parallel result = $parResult")
      println(s"parallel count time: $fjtime")
      println(s"speedup: ${seqtime.value / fjtime.value}")
    catch
      case e: NotImplementedError =>
        println("Not implemented.")

    println("\n# Using moneyThreshold\n")
    measureParallelCountChange(ParallelCountChange.moneyThreshold(amount))
    println("\n# Using totalCoinsThreshold\n")
    measureParallelCountChange(ParallelCountChange.totalCoinsThreshold(coins.length))
    println("\n# Using combinedThreshold\n")
    measureParallelCountChange(ParallelCountChange.combinedThreshold(amount, coins))

object ParallelCountChange extends ParallelCountChangeInterface:
  type Threshold = (Int, List[Int]) => Boolean

  /** Returns the number of ways change can be made from the specified list of
   *  coins for the specified amount of money */
  def countChange(money: Int, coins: List[Int]): Int =
    parCountChange(money, coins, combinedThreshold(money, coins))

  /** In parallel, counts the number of ways change can be made from the
   *  specified list of coins for the specified amount of money */
  def parCountChange(money: Int, coins: List[Int], threshold: Threshold): Int =
    // No Ways To Make Change
    if (coins.isEmpty || money < coins.head) then 
      if money!=0 then 0 else 1

    // Parallel Iter
    else if threshold(money, coins) then
      val (c1, cl) = parallel(
        // Count Change With First Coin
        parCountChange(money-coins.head, coins, combinedThreshold(money-coins.head, coins)),
        // Count Change With Rest Of Coins
        parCountChange(money, coins.tail, combinedThreshold(money, coins))
      )
      c1 + cl

    // One Way To Make Change
    else 1

  /** Threshold heuristic based on the starting money. */
  def moneyThreshold(startingMoney: Int): Threshold =
    startingMoney match {
      // startingMoney > allCoins.head
      case m if m==0 => (money, coins) => false
      case _ => (money, coins) => true
    }

  /** Threshold heuristic based on the total number of initial coins. */
  def totalCoinsThreshold(totalCoins: Int): Threshold = 
    (money, coins) => true

  /** Threshold heuristic based on the starting money and the initial list of coins. */
  def combinedThreshold(startingMoney: Int, allCoins: List[Int]): Threshold =
    (money, coins) => {
        moneyThreshold(startingMoney)(money, coins) &&
        totalCoinsThreshold(allCoins.length)(money, coins)
      }

















