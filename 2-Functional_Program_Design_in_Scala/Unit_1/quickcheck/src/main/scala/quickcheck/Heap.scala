package quickcheck

trait IntHeap extends Heap:
  override type A = Int
  override def ord = scala.math.Ordering.Int

// http://www.brics.dk/RS/96/37/BRICS-RS-96-37.pdf

// Abstract Heap 
trait Heap:
  type H // type of a heap
  type A // type of an element
  def ord: Ordering[A] // ordering on elements
  def empty: H // the empty heap
  def isEmpty(h: H): Boolean // whether the given heap h is empty
  def insert(x: A, h: H): H // the heap resulting from inserting x into h
  def meld(h1: H, h2: H): H // the heap resulting from merging h1 and h2
  def findMin(h: H): A // a minimum of the heap h
  def deleteMin(h: H): H // a heap resulting from deleting a minimum of h

// Extend Heap with instance of Binomial Heap
// Binomial Heap is a Binomal Tree where 
	// the minimal element is its root and
	// there is at most one Binomial Tree of any degree
trait BinomialHeap extends Heap:
	
  // Binomial Heap Specifics
  type Rank = Int // Inclusive number of element on every previous branch
  case class Node(x: A, r: Rank, c: List[Node]) // elements of Binomial Heap
  override type H = List[Node] // This Heap is an ordered list of its Nodes

  // Node Methods
  protected def root(t: Node) = t.x // Base element of this Heap
  protected def rank(t: Node) = t.r // Gets the rank of a given Heap
  protected def link(t1: Node, t2: Node): Node = // Constructs a New Node with two given Nodes
    if (ord.lteq(t1.x, t2.x)) Node(t1.x, t1.r + 1, t2 :: t1.c) else Node(t2.x, t2.r + 1, t1 :: t2.c)
  protected def ins(t: Node, ts: H): H = ts match // Inserts New Node Into an Existing Heap
    case Nil => List(t) // Construct Heap with one Node
    case first :: rest => // If Heap is nonEmpty
      if t.r <= first.r then t :: first :: rest // If considered Node is less than Heaps' Append 
      // else if t.r == first.r then first :: rest // If considered Node is the Heaps' minimal return Heap
      else ins(link(t, first), rest) // Make a recursive call to insert on the new node and a reduced Heap

  // Binomial Heap Methods
  override def empty = Nil // Defined Method to return an empty Heap 
  override def isEmpty(ts: H) = ts.isEmpty // Determines if heap is empty
  override def insert(x: A, ts: H) = ins(Node(x, 0, Nil), ts) // Construct Node of Type A and Insert in Heap
  override def meld(ts1: H, ts2: H) = (ts1, ts2) match // Merge 2 Heaps 
    case (Nil, ts) => ts // If left is Empty return right
    case (ts, Nil) => ts // If right is Empty return left
    case (t1 :: ts1, t2 :: ts2) => // Else Decouple left and right
      if t1.r < t2.r then t1 :: meld(ts1, t2 :: ts2) // Construct heap with a minimal head node and recurse
      else if t2.r < t1.r then t2 :: meld(t1 :: ts1, ts2)
      else ins(link(t1, t2), meld(ts1, ts2)) // Considered have the same rank, combine in new node and recurse
  override def findMin(ts: H) = ts match // Get minimal element of Heap
    case Nil => throw new NoSuchElementException("min of empty heap")
    case t :: Nil => root(t) // Single Node Heap
    case t :: ts =>
      val x = findMin(ts) // Recurse to find minimal element in subsequent Nodes
      if ord.lteq(root(t), x) then root(t) else x // If considered Nodes root is min else Heaps' min
  override def deleteMin(ts: H) = ts match // Remove minimal Element from Heap
    case Nil => throw new NoSuchElementException("delete min of empty heap")
    case t :: ts => 
      def getMin(t: Node, ts: H): (Node, H) = ts match // Helper to recurse over Heap 
        case Nil => (t, Nil)
        case tp :: tsp =>
          val (tq, tsq) = getMin(tp, tsp)
          if ord.lteq(root(t), root(tq)) then (t, ts) else (tq, t :: tsq)
      val (Node(_, _, c), tsq) = getMin(t, ts)
      meld(c.reverse, tsq) // Construct new heap from removed Node


// Cases for QuickCheckSuite, make sure Binomial Heap satisfies all its properties
trait Bogus1BinomialHeap extends BinomialHeap: // Finds minimal element in head node, but not subsequent Nodes
  override def findMin(ts: H) = ts match
    case Nil => throw new NoSuchElementException("min of empty heap")
    case t :: ts => root(t)

trait Bogus2BinomialHeap extends BinomialHeap: // Links elements incorrectly
  override protected def link(t1: Node, t2: Node): Node = 
    if !ord.lteq(t1.x, t2.x) then Node(t1.x, t1.r + 1, t2 :: t1.c) else Node(t2.x, t2.r + 1, t1 :: t2.c)

trait Bogus3BinomialHeap extends BinomialHeap: // Constructs Node List incorrectly
  override protected def link(t1: Node, t2: Node): Node = 
    if ord.lteq(t1.x, t2.x) then Node(t1.x, t1.r + 1, t1 :: t1.c) else Node(t2.x, t2.r + 1, t2 :: t2.c)

trait Bogus4BinomialHeap extends BinomialHeap: // Doesnt find minimal Node, removes minimal from first in Heap
  override def deleteMin(ts: H) = ts match
    case Nil => throw new NoSuchElementException("delete min of empty heap")
    case t :: ts => meld(t.c.reverse, ts)

trait Bogus5BinomialHeap extends BinomialHeap: // Merges Heaps without considering their rank and placement
  override def meld(ts1: H, ts2: H) = ts1 match
    case Nil => ts2
    case t1 :: ts1 => List(Node(t1.x, t1.r, ts1 ++ ts2))
