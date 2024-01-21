package calculator

// Instantiation of a Tweet
object TweetLength extends TweetLengthInterface:
  final val MaxTweetLength = 140 // Limit of a Tweets' length

  // Keeps an up-to-date count of remaining characters in a tweet
  def tweetRemainingCharsCount(tweetText: Signal[String]): Signal[Int] =
    Signal(MaxTweetLength-tweetLength(tweetText()))
  // Defines a color based on the tweets remaining characters state
  def colorForRemainingCharsCount(remainingCharsCount: Signal[Int]): Signal[String] =
    remainingCharsCount.currentValue match {
      case char if char>=15 => Signal("green") 
      case char if char<15 & char>=0=> Signal("orange")
      case _ => Signal("red")
    } 
  // Computes the length of a tweet, given its text string
  private def tweetLength(text: String): Int = 
    if text.isEmpty then 0
    else
      text.length - text.init.zip(text.tail).count(
          (Character.isSurrogatePair _).tupled)
