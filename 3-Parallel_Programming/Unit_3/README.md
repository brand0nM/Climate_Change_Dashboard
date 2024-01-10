# Data Parallelism
We show how data parallel operations enable the development of elegant data-parallel code in Scala. We give an overview of the parallel collections hierarchy, 
including the traits of splitters and combiners that complement iterators and builders from the sequential case.

## Data-Parallel Programming
*Know:* how to express task-parallel programs in terms of `tasks` and `parallel` arguments. 

*Now:* how to express data-parallel programs (A form of parallelization that distributes data across computing nodes)

### Data-Parallel Programming Model
Simplest form of data-parallel programming is the parallel for loop (eg: Initializing array values). Adding par to range converts to a parallel range, different iterations
will be executed in parallel on different processors. Since there are side effects to this implementation, only "correct" if the iterations write to seperate memory 
locations in some synchronization (i.e. not dependent on another segment).

	def initializeArray(xs: Array[Int])(v: Int): Unit = 
		for (i <- 0 (until xs.length).par)
			xs(i) = v
			// xs(0) = i // is improperly synchronized and will not work

### Example: Mandelbrot Set
Although Simple Parallel for loops allow writing interesting programs. 

Render a set of complex numer in the plane for which the sequence `Z_n+1 = Z^2_n + c` does not approach infinity

	def computePixel(xc: Double, yc: Double, maxIterations: Int): Int = 
		val i = 0
		var x, y = 0.0, 0.0
		while (x*x + y*y < 4 && i < maxIterations)
			val xt = x*x - y*y + xc
			val yt = 2*x*y + yc
			x = xt; y = yt
			i += 1
		color(i)
	def parRender(): Unit =
		for (idx <- (0 until image.length).par)
			val (xc, yc) = coordinatesFor(idx)
			image(idx) = computePixel(xc, yc, maxIterations)

### Workloads
Different data-parallel programs have different workloads. * Workload* is a function that maps each input element to the amount of work required to process it.

#### Uniform Workloads
Define a constant function `w(i) = const`. Easy to parallelise

#### Irregular Workload
Defined by an arbitrary function `w(i) = f(i)`. In the mandelbrot case `w(i) = iterations`; load depends on the problem instance.

Goal of data-parallel-scheduler: efficiently balance workload across processors without any knowledge about w(i).

## Data-Parallel Operations I
In scala, most collections can become data-parallel collections. use the `.par` method to convert a sequential collection to a parallel collection

	(1 until 1000).par
		.filter(n => n%3 == 0)
		.count(n => n.toString == n.toString.reverse)

### Non-Parallelizable Operations
Task: implementat the method `sum` using `foldLeft` method. can't work in parallel because needs acc

	def sum(xs: Array[Int]): Int =
		xs.foldLeft(0)(_+_)

### Fold Operation
Next, fold signature

	def fold(z: A)(f: (A,A) => A): A

This implementation can be computed in poarallel by computing f on 2 different sets of elements of type A. There are many ways to reconstruct these parallel tasks because
They all map to an element of type A and take an element of type A.

## Data-Parallel Operations II
### Fold use cases
Both functions below have an identity (fixed point) element, and are associative

	def sum(xs: Array[Int]): Int =
		xs.par.fold(0)(_+_)

Implement the `max` method

	def max(xs: Array[Int]): Int
		xs.par.fold(Int.MinValue)(math.max)

Given a list of paper rock and sissors strings find out who won

	Array("paper", "rock", "paper", "scissors")
		.par.fold("")(play)

	def play(a: String, b: String): String = List(a, b).sorted match
		case List("paper", "rock") => "paper"
		case List("paper", "scissors") => "scissors"
		case List("rock", "scissors") => "rock"
		case List(a, b) if a == b => a
		case List("", b) => b

	play(play("paper", "rock"), play("paper", "scissors")) == "scissors"

but scheduler is allowed to organize reduction tree differently. Could 

	play("paper", play("rock", play("paper", "scissors"))) == "paper"

Hence the result depends on execution schedule. Problem is play operator is not associative.
To produce the "correct" result- reproducable in this case- the neutral element z and binary operator f must form a *monoid*. i.e.

1) f(a, f(b, c)) == f(f(a, b), c)

2) f(z, a) == f(a, z) == a

communativity does not matter for `fold`- the following relation is not necessary `f(a, b) == f(b, a)`

### Limitations of the fold Operation

	Array('E', 'P', 'F', 'L'). par
		.fold(0)((count, c) => if (isVowel(c)) count +1 else count)

- Program does not compile: map of (A, B) => A, but fold requires (A, A) => A

### Aggregate Operaation
Lets examine the aggregate signature

	def aggregate[B](z: B)(f: (B, A) => B, g: (B, B) => B): B
	Array('E', 'P', 'F', 'L'). par.aggregate(0)(
		(count, c) => if (isVowel(c)) count=1 else count,
		 _+_
	)

### Transformer Operations
so far have seen only *accessor combinators* (sum, fold, count, max, aggregate, ...)

*Transformer combinators* (map, filter, flatMap, and groupBy), do not return a single value- instead return new collection as results.

## Scala Parallel Collections
### Scala Collection Hierarchy
- Traverable[T]- collection of elements with type T, with operations imlemented using foreach

- Iterable[T] - collection of elemnts with type T, with operations imlemeted using iterator

- Seq[T] - an ordered collection of elements with type T

- Set[T] - a set of elements with type T ( no duplicates)

- Map[K, V] - a map of keys with type K associated with values of type V, no duplicated keys

### Parallel Collection Hierarchy
ParIterable, ParSeq, ParSet, ParMap, are the counterparts of different sequential traits.

For code that is agnostic about parallelism, there exists a seperate hierachy of generic collection traits GenIterable, GenSeq, GenSet, GenMap

### Writing Parallelism-Agnsotic Code
Allows compiler to use normal and parallel traverables 

	def largestPalindrome(xs: GenSeq[Int]): Int =
		xs.aggragate(Int.MinValue)(
			(largest, n) =>
				if (n > largest && n.toString == n.toString.reverse) n else largest,
				math.max)
		)

### Non-Parallelizable Colelctions
A sequential colelciton can be converted into a parallel one by calling par.

### Parallelizable Collections
- ParArray- parallel array of objects, counterpart of Array and ArrayBuffer

- parRange- Parallel Range of integers, counterpart of Range

- Parvector- parallel vector, counteropaprt of Vector

- immutable.ParHashSet - counterpart of immutable.HashSet

- immutable.ParHashMap - counterpart of immutable.HashMap

- mutable.ParHashSet - counterpart of mutable.HashSet

- mutable.ParHashMap - counterpart of mutable.HashMap

- ParTreeM ap - thread sfae parallel map with atomic snapshots, counterpart of TreeMap

- for other colelctions, par creates the closest parallel collection - e.g. a List is convertred to a ParVector

Pick data Structer wisely as if needs parallization opertions, will be a heavy lift to convert type to such

### Compute Set Intersection

	def intersection(a: GenSet[int], b: GenSet[Int])): Set[Int] =
		val result = mutable.Set[Int]()
		for (x <- a) if (b.contains x) result += x
		result

	intersection((0 until 1000).toSet, (0 until 1000 by 4).toSet)
	intersection((0 until 1000).par.toSet, (0 until 1000 by 4).par.toSet)

This program is not correct because its trying to append a mutable data structure

*Rule:* Avoid mutable to the same memory locations without proper synchronization (in this example mutable lives in the same memory location; i.e. two invocations of +=
can act on the same memory location and run asynchroniously => leaving the set with inconsistent state)

	import java.util.concurrent._
	def intersection(a: GenSet[int], b: GenSet[Int])): Set[Int] =
		val result = new ConcurrentSkipListSet.Set[Int]()
		for (x <- a) if (b.contains x) result += x
		result

	intersection((0 until 1000).toSet, (0 until 1000 by 4).toSet)
	intersection((0 until 1000).par.toSet, (0 until 1000 by 4).par.toSet)

### Avoid Side-Effects
Side Effects can be avoided by using the correct combinators. For example, we can use filter to compute the intersection

	def intersection(a: GenSet[int], b: GenSet[Int])): Set[Int] =
		if (a. size < b.size) a.filter(b(_))
		else b.filter(a(_))

	intersection((0 until 1000).toSet, (0 until 1000 by 4).toSet)
	intersection((0 until 1000).par.toSet, (0 until 1000 by 4).par.toSet)

### Concurrent Modifications During traversals
Never modify parallel lcolelction on which a dat-parallel operationn is in progress

	val graph = mutable.Map[Int, Int]() ++= (0 until 100000).map(i => (i, i+1))
	graph(graph.size - 1) = 0
	for ((k, v) <- graph.par) graph(k) = graph(v)
	val violation = graph.find({case (i, v) => v != (i+2)% graph.size})
	println(s"violation: $violation")

In this program, the graph is contracted in parallel; i.e. each nodes successor is replaced with the successors successor. Violation is to handle boundary cases

2 errors with this program 

1) modifying the data structure we are traversing in parallel; can cause program to crash because could be observed in a corrupted state

2) Reading from collection that is currently being modified by another iteration of the loop

Lessons:
- never write to a collection that is concurrently traversed

- never read from a collection that is concurrently modified

either case program wil be non-deterministic, printing diffrent results or crashing all together

TreeMap is an exception to these rules, snapshot can be used to efficeintly grab current state. creates a clone of map and gets state at that time. So when for loop
starts, graph is atomically captured. I.e. previous and parallizes version (traverasble0 are not modified. Only the graph object is changed while iterated over

	val graph = concurrent.TreeMap[Int, Int]() ++= (0 until 100000).map(i => (i, i+1))
	graph(graph.size - 1) = 0
	val previous = graph.snapshot()
	for ((k, v) <- graph.par) graph(k) = previous(v)
	val violation = graph.find({case (i, v) => v != (i+2)% graph.size})
	println(s"violation: $violation")

## Splitters and Combiners
### Data-Parallel Abstractions
#### Iterators
simplified iterator trait is as follows

	trait Iterator[A]
		def next(): A
		def hasNext: Boolean

	def iterator: Iterator[A]

The Iterator contract
- next can ba called only if hasNext returns true
- after hasnext returns false it will always return false

How would you implement `foldLeft` on an iterator

	def foldLeft[B](z:B)(f: (B, A) => B): B extends Iterator[A] =
		var result = z
		while (hasNext) result = f(result, next())
		result

#### Splitter
Simplified `Splitter` trait is as follows

	trait Splitter[A] extends Iterator[A] 
		def split: Seq[Splitter[A]]
		def remaining: int

Splitter contract
- after calling split, the original splitter is left in an undefinied state

- the resulting splitter traverse disjoint subsets of the original splitter

- remaining is an estimate on the number of remaing elements

- split is an efficient method - O(logn) or better

How would you implement fold on splitter?

	def fold(z: A)(f: (A,A) => A): A =
		if (remaining < threshold) foldLeft(z)(f)
		else
			val children: Seq[Task[T]] = for (child <- split) yield task {child.fold(z)(f)}
			children.map(_.join()).foldLeft(z)(f)

#### Builder
simplified Builder trait as follows

	trait builder[A, Repr]
		def +=(elem: a0; Builder[A, Repr]
		def result: Repr

	def newBuilder: Builder[A, Repr]

Builder contract
- calling result reurns a collection of type Repr, containing the elements that were previously added witt+=

- calling result leaves the Builder in an undefined state

Implement filter on newBuilder

	def filter(p: T => Boolean): Repr =
		val b = newBuilder
		for (x <- this); if p(x); yield b+=x
		b.result

#### Combiner
simplified Combiner trait is as follows:

	trait Combiner[A, Repr] extends Builder[A, Repr]
		def combine(that: Combiner[A, Repr]): Combiner[A, Repr]

	def newCombiner: Combiner[T, Repr]

combiner contract
- calling combine returns a new combiner that contains elements of input combiners

- calling combine leaves voth original Combiner in an undefiend state

- combine is an efficient method - O(logn) or better

Implement filter using splitter and newCombiner in parallel

	def filter(p: T => Boolean): Repr = ...........

