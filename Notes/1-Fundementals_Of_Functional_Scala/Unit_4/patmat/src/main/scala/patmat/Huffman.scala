package patmat


abstract class CodeTree
case class Fork(left: CodeTree, right: CodeTree, chars: List[Char], weight: Int) extends CodeTree
case class Leaf(char: Char, weight: Int) extends CodeTree


trait Huffman extends HuffmanInterface:

  // Part 1: Basics
  def weight(tree: CodeTree): Int = tree match
    case Leaf(char, wait) => wait
    case Fork(l, r, chars, wait) => wait

  def chars(tree: CodeTree): List[Char] = tree match
    case Leaf(char, weight) => List(char)
    case Fork(l, r, charls, weights) => charls

  def makeCodeTree(left: CodeTree, right: CodeTree) =
    Fork(left, right, chars(left) ::: chars(right), weight(left) + weight(right))


  // Part 2: Generating Huffman trees
  def string2Chars(str: String): List[Char] = str.toList

  def times(chars: List[Char]): List[(Char, Int)] =
    chars.toSet.toList.map(setim =>
      (setim, chars.count(charitm => charitm==setim)))

  def makeOrderedLeafList(freqs: List[(Char, Int)]): List[Leaf] = 
    freqs.sortBy(_._2).map(tup => Leaf(tup._1, tup._2))

  def singleton(trees: List[CodeTree]): Boolean = trees match
    case codetree :: Nil => true
    case _ => false

  def combine(trees: List[CodeTree]): List[CodeTree] = trees match
    case x1 :: x2:: tail => List(makeCodeTree(x1, x2))++tail
    case _ => trees

  def until(done: List[CodeTree] => Boolean, merge: List[CodeTree] => List[CodeTree])
  (trees: List[CodeTree]): List[CodeTree] = 
    if done(trees) then trees
    else if weight(trees.head) <= weight(trees.tail.head) then
      until(done, merge)(merge(trees))
    else
      until(done, merge)(merge(trees.head :: merge(trees.tail)))

  def createCodeTree(chars: List[Char]): CodeTree = until(singleton, combine)(makeOrderedLeafList(times(chars))).head


  // Part 3: Decoding
  type Bit = Int
  def decode(tree: CodeTree, bits: List[Bit]): List[Char] =
    def untillMessage(subtree: CodeTree, bits: List[Bit], message: List[Char]): List[Char] = subtree match {
      case Leaf(c, w) => untillMessage(tree, bits, c :: message)
      case Fork(l, r, c, w) => 
        if bits.isEmpty then message 
        else if bits.head==0 then untillMessage(l, bits.tail, message) 
        else untillMessage(r, bits.tail, message)
    }
    untillMessage(tree, bits, Nil).reverse

  val frenchCode: CodeTree = Fork(Fork(Fork(Leaf('s',121895),Fork(Leaf('d',56269),Fork(Fork(Fork(Leaf('x',5928),Leaf('j',8351),List('x','j'),14279),Leaf('f',16351),List('x','j','f'),30630),Fork(Fork(Fork(Fork(Leaf('z',2093),Fork(Leaf('k',745),Leaf('w',1747),List('k','w'),2492),List('z','k','w'),4585),Leaf('y',4725),List('z','k','w','y'),9310),Leaf('h',11298),List('z','k','w','y','h'),20608),Leaf('q',20889),List('z','k','w','y','h','q'),41497),List('x','j','f','z','k','w','y','h','q'),72127),List('d','x','j','f','z','k','w','y','h','q'),128396),List('s','d','x','j','f','z','k','w','y','h','q'),250291),Fork(Fork(Leaf('o',82762),Leaf('l',83668),List('o','l'),166430),Fork(Fork(Leaf('m',45521),Leaf('p',46335),List('m','p'),91856),Leaf('u',96785),List('m','p','u'),188641),List('o','l','m','p','u'),355071),List('s','d','x','j','f','z','k','w','y','h','q','o','l','m','p','u'),605362),Fork(Fork(Fork(Leaf('r',100500),Fork(Leaf('c',50003),Fork(Leaf('v',24975),Fork(Leaf('g',13288),Leaf('b',13822),List('g','b'),27110),List('v','g','b'),52085),List('c','v','g','b'),102088),List('r','c','v','g','b'),202588),Fork(Leaf('n',108812),Leaf('t',111103),List('n','t'),219915),List('r','c','v','g','b','n','t'),422503),Fork(Leaf('e',225947),Fork(Leaf('i',115465),Leaf('a',117110),List('i','a'),232575),List('e','i','a'),458522),List('r','c','v','g','b','n','t','e','i','a'),881025),List('s','d','x','j','f','z','k','w','y','h','q','o','l','m','p','u','r','c','v','g','b','n','t','e','i','a'),1486387)
  val secret: List[Bit] = List(0,0,1,1,1,0,1,0,1,1,1,0,0,1,1,0,1,0,0,1,1,0,1,0,1,1,0,0,1,1,1,1,1,0,1,0,1,1,0,0,0,0,1,0,1,1,1,0,0,1,0,0,1,0,0,0,1,0,0,0,1,0,1)
  def decodedSecret: List[Char] = decode(frenchCode, secret)


  // Part 4a: Encoding using Huffman tree
  def encode(tree: CodeTree)(text: List[Char]): List[Bit] = 
    def untillCode(subtree: CodeTree, message: List[Char], bits: List[Bit]): List[Bit] = subtree match {
      case Leaf(c, w) => untillCode(tree, message.tail, bits)
      case Fork(l, r, c, w) => 
        if message.isEmpty then bits 
        else if chars(l).contains(message.head) then untillCode(l, message, 0 :: bits)
        else untillCode(r, message, 1 :: bits)
    }
    untillCode(tree, text, Nil).reverse


  // Part 4b: Encoding using code table
  type CodeTable = List[(Char, List[Bit])]

  /**
   * This function returns the bit sequence that represents the character `char` in
   * the code table `table`.
   */
  def codeBits(table: CodeTable)(char: Char): List[Bit] = "a".toList.map(x => x.toByte)

  /**
   * Given a code tree, create a code table which contains, for every character in the
   * code tree, the sequence of bits representing that character.
   *
   * Hint: think of a recursive solution: every sub-tree of the code tree `tree` is itself
   * a valid code tree that can be represented as a code table. Using the code tables of the
   * sub-trees, think of how to build the code table for the entire tree.
   */
  def convert(tree: CodeTree): CodeTable = List(("a".toList(0),"a".toList.map(x => x.toByte)))

  /**
   * This function takes two code tables and merges them into one. Depending on how you
   * use it in the `convert` method above, this merge method might also do some transformations
   * on the two parameter code tables.
   */
  def mergeCodeTables(a: CodeTable, b: CodeTable): CodeTable = 
    List(("a".toList(0),"a".toList.map(x => x.toByte)))

  /**
   * This function encodes `text` according to the code tree `tree`.
   *
   * To speed up the encoding process, it first converts the code tree to a code table
   * and then uses it to perform the actual encoding.
   */
  def quickEncode(tree: CodeTree)(text: List[Char]): List[Bit] = "a".toList.map(x => x.toByte)

object Huffman extends Huffman
