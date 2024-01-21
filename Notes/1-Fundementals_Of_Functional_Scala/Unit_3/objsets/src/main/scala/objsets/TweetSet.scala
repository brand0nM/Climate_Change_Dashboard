package objsets

import TweetReader.*
import TweetReader.ParseTweets.*
import scala.language.postfixOps

/**
 * submit c-Brandon.montalvo@charter.com NCzEMVsE8ASWdc9m
 * A class to represent tweets.
 */
class Tweet(val user: String, val text: String, val retweets: Int):
  override def toString: String =
    "User: " + user + "\n" +
    "Text: " + text + " [" + retweets + "]"

abstract class TweetSet extends TweetSetInterface:
  def mostRetweeted: Tweet = descendingByRetweet.head 
  // methods kept abstract
  def filter(p: Tweet => Boolean): TweetSet
  def filterAcc(p: Tweet => Boolean, acc: TweetSet): TweetSet
  def union(that: TweetSet): TweetSet
  def descendingByRetweet: TweetList 
  def incl(tweet: Tweet): TweetSet
  def remove(tweet: Tweet): TweetSet
  def contains(tweet: Tweet): Boolean
  def foreach(f: Tweet => Unit): Unit

  // Methods to make pirates
  def merger(that: TweetSet): TweetSet
  def descendingByRetweetAcc(accList: TweetList): TweetList
  def unElem: Tweet
  def unLeft: TweetSet
  def unRight: TweetSet

class Empty extends TweetSet:
  def filter(p: Tweet => Boolean): TweetSet = filterAcc(p, Empty())
  def filterAcc(p: Tweet => Boolean, acc: TweetSet): TweetSet = acc
  def union(that: TweetSet): TweetSet = that
  def descendingByRetweet: TweetList = Nil 
  def contains(tweet: Tweet): Boolean = false
  def incl(tweet: Tweet): TweetSet = NonEmpty(tweet, Empty(), Empty())
  def remove(tweet: Tweet): TweetSet = this
  def foreach(f: Tweet => Unit): Unit = ()

  def merger(that: TweetSet): TweetSet = that
  def descendingByRetweetAcc(accList: TweetList): TweetList = accList
  def unElem: Tweet = throw new NullPointerException
  def unLeft: TweetSet = Empty()
  def unRight: TweetSet = Empty()

class NonEmpty(elem: Tweet, left: TweetSet, right: TweetSet) extends TweetSet:
  def filter(p: Tweet => Boolean): TweetSet = filterAcc(p, Empty())
  def filterAcc(p: Tweet => Boolean, acc: TweetSet): TweetSet =
    // If element passes filter
    if p(elem) then 
      // Recursive call to left and right halves, but include element in accumulator
      left.filterAcc(p, right.filterAcc(p, NonEmpty(elem, acc, Empty())))
    else 
      // Recursive call to left and right halves, but exclude element from accumulator
      left.filterAcc(p, right.filterAcc(p, acc))
  def union(that: TweetSet): TweetSet = 
  	// Basecase Is right object empty
  	if that.descendingByRetweet.isEmpty then this
  	// If nonEmpty, recursivly construct new set 
  	else this.merger(that)
  def descendingByRetweet: TweetList = this.descendingByRetweetAcc(Nil)

  // Predefined
  def contains(x: Tweet): Boolean =
    if x.text < elem.text then
      left.contains(x)
    else if elem.text < x.text then
      right.contains(x)
    else true
  def incl(x: Tweet): TweetSet =
    if x.text < elem.text then
      NonEmpty(elem, left.incl(x), right)
    else if elem.text < x.text then
      NonEmpty(elem, left, right.incl(x))
    else this
  def remove(tw: Tweet): TweetSet =
    if tw.text < elem.text then
      NonEmpty(elem, left.remove(tw), right)
    else if elem.text < tw.text then
      NonEmpty(elem, left, right.remove(tw))
    else left.union(right)
  def foreach(f: Tweet => Unit): Unit =
    f(elem)
    left.foreach(f)
    right.foreach(f)

  // Yee functions
  def merger(that: TweetSet): TweetSet =
  	// println("next "); 
  	if elem.text < that.unElem.text then
  		// print("less "); this.foreach(tweet => print(tweet.text+" ")); println(); 
  		NonEmpty(elem, 
  			left, 
  			right.merger(NonEmpty(that.unElem, Empty(), that.unRight)))
  	else if elem.text > that.unElem.text then
  		// print("greater "); this.foreach(tweet => print(tweet.text+" ")); println(); 
  		NonEmpty(elem, 
  			left.merger(NonEmpty(that.unElem, that.unLeft, Empty())), 
  			right)
  	else
  	  	// print("equal "); this.foreach(tweet => print(tweet.text+" ")); println(); 
  	  	NonEmpty(elem, 
  	  		left.merger(that.unLeft), 
  	  		right.merger(that.unRight))
  def descendingByRetweetAcc(accList: TweetList): TweetList =
  	// If first iteration, recurse on left and right halves with new List construction
  	if accList.isEmpty then 
  		left.descendingByRetweetAcc(
  			right.descendingByRetweetAcc(Cons(elem, Nil)))
  	// If considered element is greater than head element Construct new list with considered
  	else if elem.retweets > accList.head.retweets then 
  		left.descendingByRetweetAcc(
  			right.descendingByRetweetAcc(Cons(elem, accList)))
  	// If its less than, recursively 
  	else 
  		left.descendingByRetweetAcc(
  			right.descendingByRetweetAcc(
  				Cons(accList.head, NonEmpty(elem, Empty(), Empty()).descendingByRetweetAcc(accList.tail))))
  def unElem: Tweet = elem
  def unLeft: TweetSet = left
  def unRight: TweetSet = right


trait TweetList:
  def head: Tweet
  def tail: TweetList
  def isEmpty: Boolean
  def foreach(f: Tweet => Unit): Unit =
    if !isEmpty then
      f(head)
      tail.foreach(f)

object Nil extends TweetList:
  def head = throw java.util.NoSuchElementException("head of EmptyList")
  def tail = throw java.util.NoSuchElementException("tail of EmptyList")
  def isEmpty = true

class Cons(val head: Tweet, val tail: TweetList) extends TweetList:
  def isEmpty = false


object GoogleVsApple:
  val google = List("android", "Android", "galaxy", "Galaxy", "nexus", "Nexus")
  val apple = List("ios", "iOS", "iphone", "iPhone", "ipad", "iPad")

  lazy val googleTweets: TweetSet = allTweets.filter(DeviceFilter(google))
  lazy val appleTweets: TweetSet = allTweets.filter(DeviceFilter(apple))
  lazy val trending: TweetList = googleTweets.union(appleTweets).descendingByRetweet

  def DeviceFilter(deviceTypes: List[String]): Tweet => Boolean = 
    tweet => deviceTypes.exists(device => tweet.text.contains(device))


object Main extends App:
  // Print the trending tweets
  GoogleVsApple.trending.foreach(println)