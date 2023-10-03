# Data Structure for Parallel Computing
## Lecture 4.1 - Implementing Combiners
Now we'll study the implementation of transformer emthods on parallel collections.

### Builders
Builders are used in sequential collection methods: (filter, map, flatMap, groupBy).
Sequential transformer operations can be implemented generically on parallel collections, using an abstraction called builder.
(e.g. T: String, Repr: Seq[String])

	trait Builder[T, Repr]
		def +=(elem: T): this.type
		def result: Repr

EX: Assume a single processor has a sequence of strings, needed to be capitalized; to do this all the strings in the sequence need to map to a new element. To do this we'll initialize a new
Builder object, adding the capitalized versions of the strings into the builder calling `+=`. Terminating by using the `result` method, which results in a Repr.

### Combiners
Takes two Combiner constructs and uses the `combine` method to create another Combiner. 

	trait Combiner[T, Repr] extends Builder[T, Repr]
		def combine(that: Combiner[T, Repr]): Combiner[T, Repr]

How to implement an efficient version of `combine`. Consider cases of Repr, when its
- a set or map, combine represents union (e.g. (1,2,3) combine (2,3,4) => (1,2,3,4))

- a sequence, combine represents concatenation (e.g. [1,2,3] combine [2,3,4] => [1,2,3,2,3,4])

Must execute in O(logn + logm) time, where n and m are the sizes of two input combiners. 

!()["pictures/combine vizualized.png"]

turns out Array concat can only run in 2(n+m) ~ O(n+m) with its best implementation- since must scan all elements of both arrays to copy.
Typically, sets have efficient lookup, insertion and deletion
- hash tables: expected O(1)
- balanced trees: O(logn)
- linked lists: O(n)

Most set implementations do not have efficient union operation

### Sequences
Opertion complexity for sequences can vary
- mutable linked lists: O(1) prepend and append, O(n) insertion
- functional (const) list: O(1) prepend operations, everything else O(n)
- array lists: amortized O(1) append, O(1) random access, otherwise O(n)

Mutable linked lists can have O(1) concatenation, but for sequences, concatenation is O(n).

## Lecture 4.2 - Parallel Two-Phase Construction
Most data structures can be constructed in parallel using two-phase construction.
Intermediate data structure is a data structure that
- has an efficient combine method - O(logn+logm)
- has efficient += method
- can be converted to the resulting data structure in O(n/P) time

!()["pictures/two-phase-vizualized.png"]

### Example: Array Combiner
Let's implement a combiner for arrays. Two arrays cannot be efficiently concated, so

	class ArrayCombiner[T <: AnyRef,: ClassTag](val parallelism: int)
		private var numElems = 0
		private val buffer = new ArrayBuffer[ArrayBuffer[T]]
		buffers += new ArrayBuffer[T]

!()["pictures/two-phase-arrays.png"]

First, we implement the += method

	def +=(x: T) = 
		buffers.last += x
		numElems += 1
		this
	def combine(that: ArrayCombiner[T]) =
		buffers ++= that.buffers
		numElems += that.numElems
		this

O(P), assuming that buffers contains no more than O(P) nested array buffers

	def result: Array[T] =
		val array = new Array[T](numElems)
		val step = math.max(1, numElems / parallelism)
		val starts = (0 until numElems by step) :+ numElems
		val chunks = starts.zap(starts.tail)
		val tasks = for ((from, end) <- chunks) yield task
			copyTo(array, from, end)

		tasks.foreach(_.join())
		array
!()["pictures/two-phase-array-combine.png"]

### Benchmark
Demo- we will test the performance of the `aggregate` method

	xs.par.aggregate(newCombiner)(_ += _, _ combine _).result

Starts to loose its efficiency after a certain amount of parallelization

### Two-Phase Construction for Arrays
Works similarly for other data structures. First, partition the elements, then construct parts of the final data structure in parallel.
1) partition the indices into subintervals

2) initialize the array in parallel

### Two-Phase Construction for Hash Tables
1) partition the has codes into buckets (i.e. 4 buckets, takes first 2 binary elements 00 10 01 11 and backets hash codes)

2) allocate the table, and map the hash codes from different buckets into different regions (combine method here is dependent on the amount of buckets and merges each segment from differnt
processing cores; can write to the result within nonoverlapping intervals)

!()["pictures/two_phase-hasmap.png"]

### Two-Phase Construction Search trees
1) partition the elements into non-overlapping intervals according to their ordering (randomly assigning values to processors, then choosing pivot elements that act as cutoffs to bucket
elemnts on each processor)

2) construct search trees in parallel, and link non-overlapping trees (during reduction corresponding buckets get combined and result in 4 merged search trees containing elements of same 
bucket; reason they can be merged efficiently is becauseeach tree contains disjoint elements: log time)

!()["pictures/two-phase-search-trees.png"]
(can look at further reading for how to merge these trees)

### Two-Phase Construction for Spactial Data Structure
1) spatially partition the elements

2) construct non-overlapping subsets and link them

### Implementing Combiners
How can we implement combiners
1) Two Phase construction: the combiner uses an intermediate data structure with an efficient combine method to partition the eleemnts. When result is called, the final data structure is 
constructed in parallel from the intermediate data structure.

2) An efficient concatenation or union operation- a preffered way when the resulting data strucutre allows this

3) Concurrent data structure- different combiners share the same underlying data structure, and rely on *synchronization* to correctly update the data structure when += is called

## Lecture 4.3 - Conc-Tree Data Structure
### List Data Type
Let's recall the list data type in functional programming

	sealed trait List[+T]
		def head: T
		def tail: list[T]
	case class ::[T](head: T, tail: List[T])
	extends List[T]
	case object Nil extends List[Nothing]
		def head = sys.error("empty list")
		def tail = sys.error("empty list")

How do we implement a filter method on lists

	def filter[T](lst: List[T])(p: T => Boolean): List[T] = lst match
		case x :: xs if p(x) => x :: filter(xs)(p)
		case x :: xs => filter(xs)(p)
		case Nil => Nil

### Trees
Lists are built for sequential computations, they are traversed from left to right. Allow parallel computations their subtrees can be traversed in parallel

	sealed trait Tree[+T]

	case class Node[T](l: Tree[T], r: Tree[T]) extends Tree[T]
	case class Leaf[T](elem: T) extends Tree[T]
	case object Empty extends Tree[Nothing]

#### Filter on Trees
Since filter in this case reduces the tree it will become unbalanced and hence processors will have unbalanced workloads

	def filter[T](t: Tree[T])(p: T => Boolean): Tree[T] = t match
		case Node(l, r) => Node(parallel(filter(l)(p), filter(r)(p)))
		case Leaf(elem) => if (p(elem)) t else Empty
		case Empty => Empty

### Conc Data Type
trees are not good for parallelism unless they are balanced. Lets device a data type called Conc, which represents balanced trees:

	sealed trait Conc[+T]
		def level: int
		def size: int
		def left: Conc[T]
		def right: Conc[T]

In parallel programming this data type is known as the conc-list (introduced in the Fortress language)

	case object Empty extends Conc[Nothing]
		def level = 0
		def size = 0

	case object Single[T](val x: T) extends Conc[T]
		def level 0 
		def size = 1

	case object <>[T](left: Conc[T], right: Conc[T]) extends Conc[T]
		def level = 1 + math.max(left.level, right.level)
		def size = left.size + right.size

Additionally, define following invarriants for Conc-Trees to ensure they are balanced

1) A <> node can never contain Empty as its subtree 

2) The level differenece between the left and the right subtree of a <> node is always 1 or less

As consequence tree with n elements will always be bound by height logn

	def <>(that: Conc[T]): Conc[T] =
		// Ensures 1st invariant eliminating empty trees
		if (this == Empty) that
		else if (that == Empty) this
		// reorganizes tree to ensure 2nd invariant is maintained
		else concat(this, that)

	def concat(that: Conc[T]): Conc[T]
		val diff = that.level - this.level
		// case 1 trees of same height
		// Note, constructs new tree, instead of trying to rebalance xs<>ys
		if (diff >= -1 && diff <= 1) new <>(this, that)
		// case 2 trees of varying height
		// take case where left height is greater than right
		else if diff <= -1
			if (this.left.level >= this.right.level)
				// decompose xs, recursively concat right subtree and link together
				new <>(this.left, concat(this.right, that))
			else
				val con = concat(this.right.right, that)
				if (con.level == this.level - 3)
					new <>(this.left, new <>(this.right.left, con))
				else
					new <>(new <>(this.left, this.right.left), con)

What is the complexity of `<>` method, O(h1 - h2)

## Lecture 4.4 - Amortized, Constant Time Append Operation
### Constant Time Append in Conc-Trees
Lets use Conc-Trees to implement a Combiner. How can we implement `+=` method. This implementation runs in logn time, since must traverse to leaf. Is there a way to implement in constant time?

		var xs: Conc[T] = Empty
		def +=(elem:T)
			xs = xs<> Single(elem)

To achieve O(1) appends with low constant factors, we need to extend the Conc-Tree data structure. Introduce new append node with different semantics.
Now we will forget about 2nd invariant, allowing unbalanced trees as long as they have nonEmpty subtrees.

	case object append[T](left: Conc[T], right: Conc[T]) extends Conc[T]
		val level = 1 + math.max(left.level, right.level)
		val size = left.size + right.size

Possible implementation

	def appendLeaf[T](xs: Conc[T], y; T): Conc[T] = Append(xs, new Single(y))

If we want to use later in parallel must balance this new declaration. Building linked list, the quickest this could happen is O(n) time since must traverse each appended element.

### Counting in a Binary Number System
Level 0: 0 (W = 2^0) increment by 1 => (1)*2^0 == 1 == (1)

Level 1: 1 (W = 2^1 + 2^0) increment by 1 => 1*2^1 + 0*2^0 == 2 == (10)

Level 2: 2 (W = 2^1 + 2^0) increment by 1 => 1*2^1 + 2^0(1+ 0) == 3 == (11)

Level 3: 3 (W = 2^2 + 2^1 + 2^0) increment by 1 => 1*2^2 + 0*2^1 + 0*2^0 == 4 == (100)

EX: 4 (100), 8 (1000), 16 (10000)
- to count up to n in binray number system, need O(n) work

- O(logn) digits

!()["pictures/binary mapping.png"]
- to add n leaves to an Append list, we need O(n) work

- Storing n leaves requires O(logn) Append nodes

O(n)/n ~ O(1)

### Binary Representation
- 0 digit corresponds to a missing tree

- 1 digit corresponds to an existing tree

### Constant Time Append in Conc Trees

	def appendLeaf[T](xs: Conc[T], ys: Single[T]): Conc[T] = xs match
		case Empty => ys
		case xs: Single[T] => new <>(xs, ys)
		case _ <> _ => new Append(xs, ys)
		case xs: Append[T] => append(xs, ys)

	@tailrec private def append[T](xs: Append[T], ys: Conc[T]): Conc[T] =
		if (xs.right.level > ys.level) new Append(xs, ys)
		else
			val zs = new <>(xs.right, ys)
			xs.left match
				case ws @ Append(_, _) => append(ws, zs)
				case ws if ws.level <= zs.level => ws <> zs
				case ws => new Append(ws, zs)

## Lecture 4.5 - Conc-Tree Combiners
### Conc Buffers
The `ConcBuffer` appends elements into an array of size k. When array gets full, it is stored into a `Chunk` node and added into the Conc-tree

	class ConcBuffer[T: ClassTag](val k: Int, private var conc: Conc[T])
		private var chunk: Array[T] = new Array(k)
		private var chunkSize: Int = 0

The += operation in most cases just adds an element to the chunk array

	final def +=(elem: T): Unit =
		if (chunkSize >= k) expand()
		chunk(chunkSize) = elem
		chunkSize += 1

Sometimes chunk array becomes full, and needs to be expanded. Introduce a new Chunk node, similar to Single nodes, but instead of a single element they hold an array of elements

	class Chunk[T](val array: Array[T], val size: Int) extends Conc[T]
		def level = 0

The `expand` method inserts the chunk into the Conc-tree, and allocated a new chunk

	private def expand()
		conc = appendLeaf(conc, new Chunk(chunk, chunkSize))
		chunk = new Array(k)
		chunkSize = 0

`combine` method is straightforward

	final def combine(that: ConcBuffer[T]): ConcBuffer[T] =
		val combinedConc = this.result <> that.result
		new ConcBuffer(k, combinedConc)

relies on the result method to obtain Conc-trees from both buffers. Packs chunk array into the tree and returns the resulting tree

	def result: Conc[T] =
		conc = appendLeaf(conc, new Conc.Chunk(chunk, chunkSize))
		conc

#### Demo

	xs.par.aggregate(newConcBuffer[String])(_+=_, _combine_).result

