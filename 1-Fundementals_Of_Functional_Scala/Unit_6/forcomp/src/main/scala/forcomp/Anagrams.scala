package forcomp

import scala.io.{ Codec, Source }

object Anagrams extends AnagramsInterface:

  type Word = String
  type Sentence = List[Word]
  type Occurrences = List[(Char, Int)]

  val dictionary: List[Word] = Dictionary.loadDictionary

  def wordOccurrences(w: Word): Occurrences = 
    val lowW = w.toLowerCase; lowW.toSet.toList.map(uni => 
      (uni, lowW.filter(_==uni).length)).sortBy(_._1)

  def sentenceOccurrences(s: Sentence): Occurrences = wordOccurrences(s.mkString)

  lazy val dictionaryByOccurrences: Map[Occurrences, List[Word]] = 
    dictionary.groupBy(wordOccurrences)

  def wordAnagrams(word: Word): List[Word] = 
    dictionaryByOccurrences.filter((k,v)=>v.contains(word)).values.toList.flatten

  def combinations(occurrences: Occurrences): List[Occurrences] =
    occurrences.foldRight(List[Occurrences](Nil))((occ, acc) => acc ++ (
      for 
        comb<-acc
        i <- 1 to occ._2
      yield (occ._1, i) :: comb
    ))

  def subtract(x: Occurrences, y: Occurrences): Occurrences = 
    x.map(xelm =>
      val yls = y.filter(_._1==xelm._1) // filter where right has element
      if yls.isEmpty then xelm // if doesnt return left element
      else (xelm._1, xelm._2-yls(0)._2)).filter(_._2>0) // else get positive occurrences difference

  /** Returns a list of all anagram sentences of the given sentence.
   *
   *  An anagram of a sentence is formed by taking the occurrences of all the characters of
   *  all the words in the sentence, and producing all possible combinations of words with those characters,
   *  such that the words have to be from the dictionary.
   *
   *  The number of words in the sentence and its anagrams does not have to correspond.
   *  For example, the sentence `List("I", "love", "you")` is an anagram of the sentence `List("You", "olive")`.
   *
   *  Also, two sentences with the same words but in a different order are considered two different anagrams.
   *  For example, sentences `List("You", "olive")` and `List("olive", "you")` are different anagrams of
   *  `List("I", "love", "you")`.
   *
   *  Here is a full example of a sentence `List("Yes", "man")` and its anagrams for our dictionary:
   *
   *    List(
   *      List(en, as, my),
   *      List(en, my, as),
   *      List(man, yes),
   *      List(men, say),
   *      List(as, en, my),
   *      List(as, my, en),
   *      List(sane, my),
   *      List(Sean, my),
   *      List(my, en, as),
   *      List(my, as, en),
   *      List(my, sane),
   *      List(my, Sean),
   *      List(say, men),
   *      List(yes, man)
   *    )
   *
   *  The different sentences do not have to be output in the order shown above - any order is fine as long as
   *  all the anagrams are there. Every returned word has to exist in the dictionary.
   *
   *  Note: in case that the words of the sentence are in the dictionary, then the sentence is the anagram of itself,
   *  so it has to be returned in this list.
   *
   *  Note: There is only one anagram of an empty sentence.
   */
  def sentenceAnagrams(sentence: Sentence): List[Sentence] = 
    def Anagram(occ: Occurrences): List[Sentence] = 
      if occ.isEmpty then List(Nil)
      else 
        for
          comb <- combinations(occ)
          filtDic <- dictionaryByOccurrences.getOrElse(comb, Nil)
          subSet <- Anagram(subtract(occ, wordOccurrences(filtDic)))
          if comb.nonEmpty
        yield filtDic :: subSet
    Anagram(sentenceOccurrences(sentence))


object Dictionary:
  def loadDictionary: List[String] =
    val wordstream = Option {
      getClass.getResourceAsStream(List("forcomp", "linuxwords.txt").mkString("/", "/", ""))
    } getOrElse {
      sys.error("Could not load word list, dictionary file not found")
    }
    try
      val s = Source.fromInputStream(wordstream)(Codec.UTF8)
      s.getLines().toList
    catch
      case e: Exception =>
        println("Could not load word list: " + e)
        throw e
    finally
      wordstream.close()
