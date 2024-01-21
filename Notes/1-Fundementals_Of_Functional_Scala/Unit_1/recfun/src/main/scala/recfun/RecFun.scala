package recfun

object RecFun extends RecFunInterface:

  def main(args: Array[String]): Unit =
    println("Pascal's Triangle")
    for row <- 0 to 10 do
      for col <- 0 to row do
        print(s"${pascal(col, row)} ")
      println()

  /**
   * submit c-Brandon.montalvo@charter.com OiCqJl9bXhl6xoOj
   * Exercise 1
   */
  def pascal(c: Int, r: Int): Int = 
    def atTheEnds: Boolean = if c == r|| c == 0 || r == 0 then true else false
    if atTheEnds then 1 else pascal(c-1, r-1) + pascal(c, r-1)
  /**
   * Exercise 2
   */
  def balance(chars: List[Char]): Boolean =
    def newHeights(newChars: List[Char], level: Int): Int =
      // Return final level
      if newChars.isEmpty then level
      // Current element is a left parenthesis, add 1 to level
      else if (newChars.head.toString == "(" && level >= 0) then newHeights(newChars.tail, level + 1)
      // Current element is a right parenthesis, subtract 1 from level
      else if (newChars.head.toString == ")" && level >= 0) then newHeights(newChars.tail, level - 1)
      // Current element is something else, continue recursion
      else if level >= 0 then newHeights(newChars.tail, level)
      // Break recursion if ever unbalanced
      else level
    // verify that returned level is 0, meaning
    // parethesis were never unbalanced in recursion and final element balances recusive call
    if newHeights(chars, 0) == 0 then true else false

  /**
   * Exercise 3
   */
  def countChange(money: Int, coins: List[Int]): Int = 
    if coins.isEmpty then 0 
    else if money < coins.head then 0
    else if money > coins.head then countChange(money - coins.head, coins) + countChange(money, coins.tail)
    else 1 
