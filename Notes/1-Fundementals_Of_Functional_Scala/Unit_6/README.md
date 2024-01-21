# Week 6
Dive into other immutable data types like vectors, maps, ranges, arrays and more. 
Second topic are scatters-powerful and for-ocmprehensions for querying data.

## Lecture 6.1 - Other Collections
### Other Sequences
We have seen that lists are *linear*.
Access to the first element is much faster than access to the middle or end of a list.

The Scala library also defines an alternative sequcne implementation, `Vector`.
This one has more evenly balanced access patterns than `List`.

if Vector has $2^5$ elements or less, its just an array

else if Vector has less than $2^{5*2}$

...

else if Vector has less than $2^{5*5}$

else throw 

!()["Desktop/Scala_Coursera/Unit_6/Pictures/vector_composedOf_Array.png"]

Say we want to swap an element in the middle of the list, then we can insert a new subarray, 
for each layer in the trees depth.

!()["Desktop/Scala_Coursera/Unit_6/Pictures/vector_swap_element.png"]

### Operations on Vectors
Vectors are created analogously to lists:
`val nums = Vector(1, 2, 3, -88)` and `val people = Vector("Bob", "James", "Peter")`

They support the same operations as lists, with the exception of `::`.
Instead of `x :: xs`, there is
- `x +: xs`: create new vector with element x at the begining followed by elements of xs
- `x :+ xs`:  create new vector with element x at the end followed by elements of xs

**Note** the `:` always points to the sequence

### Collection Hierachy
A common base class of `List` and `Vector` is `Seq`, the class of all *sequences*.
`Seq` itself is a subclass of Iterable.

!()["Desktop/Scala_Coursera/Unit_6/Pictures/Collection_Hierarchy.pdf"]

For effiecency purposes, `Array` is a separate class, but there are automatic conversions to upcast back to `Seq`

#### Arrays and Strings
`Arrays` and `Strings` support the same operations as `Seq` and can implicity be converted to sequences where needed.

(They cannot be subclasses of `Seq` because they come from *Java*)

	scala> val xs: Array[Int] = Array(1, 2, 3).map(x => 2 * x)
	scala> val ys: String = "Hello World!".filter(_.isUpper)

#### Ranges
Another simple kind of sequcne is a `range`

it represents a sequence of evenly spaced integers. Three operators:
- to (inclusive)
- until (exclusive)
- by (determines the step value) 

	scala> var: Range = 1 until 5 // (1,2,3,4)
	scala> var s: rangel = 1 to 5 // (1,2,3,4,5)
	scala> 1 to 10 by 3           // (1,4,7,10)
	scala> 6 to 1 by -2           // (6,4,2)

### Operations on Sequences
- xs.exists(p): true if there is an element x of xs such that p(x) holds, false otherwise
- xs.forall(p): true if p(x) holds for all elements x of xs, false otherwise
- xs.zip(ys): A sequence of pairs drawn from corresponding elements of sequences xs and ys.
If one sequence is longer than the other, it's truncated to make them the same length.
- xs.unzip: Splits a sequence of pairs xs into two sequences consisting of the first, respectively second halves of all pairs
- xs.flatMap(f): Applies collection value function f to all elements of xs and concatenates the results
- xs.sum: The sum of all elements of this numeric collection
- xs.product: The product of all elements of this numeric collection
- xs.max: The maximum of all elements of this colelction (an *ordering* must exist)
- xs.min: The minimum of all elements of this collection

### Combinations Exercise 6.1.1
To list all combinations of numbers x anmd y where x is drawn from 1..M and y is drawn from 1..N:

	scala> (1 to M).flatMap(x => (1 to N).map(y => (x, y)))

The runtime of (1 to N) is vector

### Scalar Product Exercise 6.1.2
TO ocmpute the scalar product of two vectors:

	scala> def scalarProduct(xs: Vector[Double], ys: Vector[Double]): Double =
		xs.zip(ys).map((x, y) => x * y).sum

Automatic decomposition here.
Each pair of elements from xs and ys are split into its halves which are then passed as the x and y parameters to the lambda.

To be more explicit we could also write

	scala> def scalarProduct(xs: Vector[Double], ys: Vector[Double]): Double =
		xs.zip(ys).map(xy => xy._1 * xy._2)

Now instead of decomposing the tuple, we reference its elements directly.

To be even more concise, we could also write

	scala> def scalarProduct(xs: Vector[Double], ys: Vector[Double]): Double =
		xs.zip(ys).map(_*_).sum

In terms of runtime performance all 2 versions are essentially equivalent

#### Exercise 6.1.3
A number is **prime** if the only divisors of n are 1 and itself

Whats a highlevel way to write a test from primality of numbers? For once, value conciseness over efficiency?

	scala> def isPrime(n: Int): Boolean = (2 until n).forall(n%_=0)

#### Practice Quiz 6.1
1) Select all the statements that are true of Scala sequences (type 'Seq').

	- Although 'String' and 'Array' are Java types, they support the same operations as Scala sequences ('map', 'filter', etc.).

		Correct, Indeed, Java 'String' and 'Array' are enriched with the same operations as Scala sequences.

	- All the types of sequences share a common rich set of operations ('map', 'filter', 'flatMap', 'zip', 'forall', 'sum', etc.).

		Correct, Yes!

	- All the types of sequences support efficient random access.

	- Sequences can contain duplicates (several elements with the same value).

		Correct, Good job! This is not true of all the Scala collections, but sequences can contain duplicates.

2) What does the following program compute?

	def mystery(xs: Seq[Int]) =
		xs
			.filter(x => x % 2 == 0)
			.map(x => x * x)
			.sum	

	- It returns a collection containing the square of the even numbers of the provided sequence.

	- It computes the sum of the even numbers of the provided sequence.

	- It returns 'true' if all the elements of the provided sequence are even numbers.

	- It computes the sum of the squares of the even numbers of the provided sequence.

		Correct, Good job!

## Lecture 6.2 - Combinatorial Search and For-Expressions
### Handling Nested Sequences
We can extend the usage of higher order functions on sequences to many calculations which are usually expressed using nested loops.

**Example:** given a positive integer n,
find all pairs of positive integers `i` and `j`,
with `1 <= j < i < n` such that ` i + j` is prime.

if n=7, the sought pairs are
	
	i  | 2 3 4 4 5 6 6
	j  | 1 2 1 3 2 1 5
	___________________
	i+j| 3 5 5 7 7 7 11

### Algorithm
A natural way to do this is to:
- generate the sequence of pairs `(i, j)` when `1 < j <= i < n`
- Filter paris for which `i + j` is prime

One natural way to generate the sequence of pairs is to:
- Generate all integers i between 1 and n (excluded)
- For each integer i, generate the list of pairs (i, 1), ..., (i, i-1)

	scala> (1 until n).map(i => (1 until i).map(j => (i, j)))

### Generate Pairs
The previous step gave a sequence of sequences, let's call it xss.

We can combine all the sub-sequences using `foldRight` with `++`:

	scala> xss.foldRight(Seq[Int]())(_ ++ _)

Or, equivalently, we use the built-in ethod `flatten`

	scala> xss.flatten

This gives 

	scala> (1 until n).map(i => (1 until i).map(j => (i, j))).flatten

Here's a useful law: ` xs.flatMap(f) = xs.map(f).flatten`. Hence, the aboce expression can be simplified to 

	scala> (1 until n).flatMap(i => (1 until i).map(j => (i, j))

### Assembling the pieces
By reassembling the pieces, we obtain the following expression:

	scala> (1 until n).flatMap(i => (1 until i).map(j => (i, j)).filter((x, y) => isPrime(x + y))

This works, but is unreadable.

Higher-order functions such as `map`, `flatMap` or `filter` provide powerful constructs for manipulating lists.
But sometimes the level of abstraction required by these functions makes the program difficult to understand

Leads to for-Expressions

### For-Expression Example
Let persons be a list of elements of class `Person`, with fields `name` and `age`.

	scala> case class Person(name: String, age: int)

TO obtain the names of persons over 20 years, can write:

	scala> for p <- persons if p.age > 20 yild p.name

which is equivalent to:

	scala> persons
		.filter(p => p.age > 20)
		.map(p => p.name)

the for-expression is similar to loops in imperative languages, except that it builds a list of the results of all iterations.

### Syntax of For
A for-expression is of the form `for s yield e`. 

Where s is a sequence of **generators** and **filters**, and e is an expression whose value is returned by an iteration.
- A **generator** is of the form p <- e, where p is a pattern and e an expression whose value is a collection.
- A **filter** is of the form if f, where f is a `boolean` expression
- The sequence must start with a generator
- If there are several generators in the sequence, the last generators vary faster than the first

### Use of For
Here are two examples which were previously solved with higher-order functions:

Given a positive integer n, find all the pairs of positive integers `(i, j)` such that `1 <= j < i < n`, and `i + j` is prime.

	scala> for
		i <- 1 until n
		j <- 1 until n
		if isPrime(i + j)
	yield (i, j)

#### Exercise
Write a version of scalarProduct thta makes use of a for:

	scala> def scalarProduct(xs: Vector[Double], ys: Vector[Double]): Double =
		(for (x, y) <- xs.zip(ys) yield x * y).sum

Question:** What will the following produce?

	(for x <- xs; y <- ys yield x * y).sum => y1(xs) + ... + yn(xs)

#### Practice Quiz 6.2
1) Which of the loops below implement the same calculation?

	val data = List(1, 1, 2, 3, 5, 8)

	def loopA(values: List[Int]) =
		for x <- values yield x * x

	def loopB(values: List[Int]) =
		(0 to (values.size)).map { x =>
			x * x
		}

	def loopC(values: List[Int]) =
		values.map(x => x * x)

	- loopA

		Correct

	- loopB

	- loopC

		Correct

2) Consider the following program.

	def mystery(xs: Seq[Int]) =
		xs
			.filter(x => x % 2 == 0)
			.map(x => x * x)
			.sum

Which of the following is equivalent to this one

	def mystery(xs: Seq[Int]) =
		val ys =
			for x <- xs
			if x % 2 == 0
			yield x * x
		ys.sum

## Lecture 6.3 - Combinatorial Search Example
### Sets
Sets are another basic abstraction in the Scala collections.
A set is written analogously to a sequence:

	scala> val fruit = Set("apples","banana","pear")
	scala> val s = (1 to 6).toSet

Most operations on sequences are also available on sets:

	scala> s.map(_ + 2)
	scala> fruit.filter(_.startsWith("app"))
	scala> s.nonEmpty

See `Iterables` Scaladoc for a list of all supported operations

#### Sets vs Sequences
The principal differneces between sets and sequences are:

1) Sets are unordered; the elements of a set do not have a predefined order in which they appear in the set

2) sets do not have duplicate elements: `s.map(_ / 2)`, Set(2,0,3,1)

3) The fudemental operation on sets is contains: `s.contains(5)`, true

### Example: N-Queens
The eight queens problem is to place eight queens on a chessboard so that no queen is threatened by another.
- In other words, there can't be two queens in the same row, column, or diagonal

We no develop a solution for a chessboard of any size not just 8.
One way to solve the problem is to place a queen on each row.

Once we have placed k-1 queens, 
one must place the kth queen in a column where it's not "in check" with any other queen on the board.

#### Algoritm
We can solve this problem wth a recursive soln:
- Suppose that we have already generated all solutions consisting of placing k-1 queens on a board of size n
- Each solution is represented by a list (of length k-1) containing the numbers of columns (between 0 and n-1)
- The column number of the queen in the k-1th row comes first in the list, follwed by the column number of the queen in row k-2, etc
- The solution set is thus represented as a setof lists with one element for each solution
- Now, to place the kth queen, we generate all possible extensions of each solution preceded by a new queen:

#### Implementation

	scala> def queens(n: Int) =
		def placeQueens(k: Int): Set[List[Int]] =
			if k == 0 then Set(List())
			else
				for
					queens <- placeQueens(k - 1)
					col <- 0 until n
					if isSafe(col, queens)
				yield col :: quens
			placeQueens(n)

#### Exercise 6.3.1
Write a function. Which tests if a queen placed in an indicated column col is secure amongst the other placed queens.

its assumed that the new queen is placed in the next availble row after the other placed queens(or in row queens.length)

	scala> def isSafe(col: Int, queens: List[Int]): Boolean =
		queens.filter(p => p==col).length==1 then true else false && diagCheck(col, 1, queens)

		def diagCheck(col: Int, delta: Int, queens: List[Int]): Boolean = queens match
			case qcol :: others =>
				(qcol - col).abs == delta
				|| diagCheck(col, delta + 1, others)
			case Nil => true 

#### Practice Quiz 6.3
1) Select all the statements that are true of Scala sets (type 'Set').

	- Sets support efficient random access

	- Most of the operations of sequences are also available on sets ('map', 'filter', 'flatMap', 'zip', 'forall', 'sum', etc.).

		Correct, These methods are all defined on the base collection type 'Iterable'

	- Unlike sequences, sets do not have duplicate elements.

		Correct

	- Sets are not ordered by index (the elements of a set do not have a predefined order in which they appear in the set)

		Correct

2) What is the output of the following program?

	val xs: Seq[Int] = (1 to 10).map(_ => 42)
	val ys: Set[Int] = xs.to(Set)
	println(xs)
	println(ys)

	Vector(42, 42, 42, 42, 42, 42, 42, 42, 42, 42)
	Set(42)

## Lecture 6.4 - Maps
### Map
Another fundemental collection type is the ** map**

A map of type `Map[Key, Value]` is a data structure that associates keys of type `Key` with values of type `Value`.

	scala> val romanNumerals = Map("I" -> 1, "V" -> 5, "X" -> 10)
	scala> val capitalOfCountry = Map("US" -> "Washington", "Switzerland" -> "Bern")

### Maps are Iterables
Class `Map[Key, Value]` extends the collection type `Iterable[(Key, Value)]`. 
Therefore, maps support the same collection operations as other iterables do. Example:

	scala> val countryOfCapital = capitalOfCountry.map((x, y) => (y, x)) 

so maps extends `Iterables` of key/value pairs.
Syntactically `key` -> `value` is just an alternative way to write the pair (key, value). 
(-> implemeted as an extension method in `Predef`)

	scala> extension [K, V] (K:K)
		def -> (V:V) = (K, V)

### Maps are Function
Class `Map[Key, Value]` also extends the function type `key => Value`, so maps can be used everywhere functions can.

In particular, maps can be applied to key arguments: `captialOfCountry("US")`

#### Querying Map
Applying a map to a non-existing key gives an error:

	scala> capitalOfCountry("Andorra") // java.util.NoSuchElementException: key not found: Andorra

To query a map without knowing beforehand whether it contains a given key, you can use the get operation:

	scala> capitalOfCountry.get("US")       // Some("Washington")
	scala> capitalOfCountry.get("Andorra")  // None

The result of a get operation is an `Option` value.


### The Option Type
The `Option` type is defined as:

	scala> trait Option[+A]
	scala> case class Some[+A](value: A) extends Option[A]
	scala> object None extends Option[Nothing]

The expression map.get(key) returns
- None: if map does not contain the given key
- Some(x): if map assocaites the given key with the value x

#### Decomposing Options
Since options are defined as case classes, they can be decompossed using pattern matching:

	scala> def showCapital(country: String) = capitalOfCountry.get(country) match
		case Some(capital) => capital
		case None => "missing data"

	scala> showCapital("US")      // Washington
	scala> showCapital("Andorra") // "missing data"

`Options` also support quite a few operations of the other collections; I invite you to try them out.

### Updating Maps
Functional updates of a map are done with the + and ++ operations:

	- m + (k -> v): The map that take key 'k' to value 'v' and is otherwise equal to 'm'
	- m ++ kvs: The map 'm' updated via '+' with all key/value pairs in 'kvs'

These operations are purely functional. For instance,

	val m1 = Map("red" -> 1, "blue" -> 2)     // m1 = Map(red -> 1, blue -> 2)
	val m2 = m1 + ("blue" -> 3)                // m2 = Map(red -> 1, blue -> 3)
	m1                                        // Map(red -> 1, blue -> 2)          

### Sorted and GroupBy
Two useful operations known from SQL queries are `groupBy` and ` orderBy`. `orderBy` on a collection can be expressed 
using `sortedWith` and `sorted`.

	scala> val fruit = List("apples", "banana", "pear", "pineapple") 
	scala> fruit.sortWith(_.length < _.length)                            // List("pear", "apples", "banana", "pineapple")
	scala> fruit.sorted                                                   // List("apples", "banana", "pear", "pineapple")

`groupBy` is available on Scala collections. It partitions a collection into a map of collections according to *disciminator fucntion* `f`

	scala> fruit.groupBy(_.head)                          // HashMap(a -> List(apples), b -> List(banana), p -> List(pear, pineapple))

#### Map Example
A polynomial can be seen as a map from exponents to coefficients.

For instance $x^3 - 2x + 5$ can be represented with the map.
`Map(0 -> 5, 1 -> -2, 3 -> 1)`

Based on this obsevation, let's design a class `Polynom` that represents polynomials as maps.

### Default Values
So far, maps were **partial functions**: Applying a map to a key value in map(key) could lead to an exception,
if the key was not stoted in the map.

There is an operation `withDefaultValue` that turns a map into a total function:

	scala> val cap1 = capitalOfCountry.withDefaultValue("<unkown>")
	scala> cap1("Andorra")                     // "<unknown>"

#### Exercise 6.4.1

	scala> class Polynom(nonZeroTerms: Map[Int, Double]):
		def terms = nonZeroTerms.withDefaultValue(0)
		def + (other: Polynom): Polynom =
			Polynom(terms ++ other.terms.map((exp, coeff) => (exp, terms(exp) + coeff))) 
		override def toString = 
			val termStrings = 
				for (exp, coeff) <- terms.toList.sorted.reverse
				yield
					val exponent = if exp==0 then "" else "x^"+exp
					coeff + exponent
			if terms.isEmpty then "0" 
			else termStrings.mkString(" + ")
	scala> val x = Polynom(Map(0 -> 2, 1 -> -3, 2 -> 1))                                          // Polynom = 1.0x^2 - 3.0x^1 + 2.0
	scala> val z = Polynom(Map())                                                                 // Polynom = 0
	scala> x + x + z                                                                              // Poloynom = 2.0x^2 - 6.0x^1 + 4.0
 
### Variable Length Argument Lists
It's quite inconvenient to have to write `Polynom(Map(1 -> 2.0, 3 -> 4.0, 5 -> 6.2))` can one do without the `Map(...)`?

Problem: The number of key -> value pairs passed to `map` can vary. We can accommodate this pattern using a **repeated parameter**

	scala> def Polynom(bindings: (Int, Double)*) =
			Polynom(bindings.toMap.withDefaultValue(0))
	scala> Polynom(1 -> 2.0, 3 -> 4.0, 5 -> 6.2)

Inside the `Polynom` function, `bindings` is seen as a `Seq[(Int, Double)]`

#### Exercise 6.4.2

	scala> class Polynom(nonZeroTerms: Map[Int, Double]):
		def this(bindings: (Int, Double)*) = this(bindings.toMap)
		def terms = nonZeroTerms.withDefaultValue(0.0)
		def + (other: Polynom): Polynom =
			Polynom(terms ++ other.terms.map((exp, coeff) => (exp, terms(exp) + coeff))) 
		override def toString = 
			val termStrings = 
				for (exp, coeff) <- term.toList.sorted.reverse
				yield
					val exponent = if exp==0 then "" else "x^"+exp
					coeff + exponent
			if terms.isEmpty then "0" 
			else termStrings.mkString(" + ")

#### Exercise 6.4.3
The `+` operation on `Polynom` used map concat with `++`. Design another version of `+` interms of `foldLeft`:

	scala> def + (other: Polynom) = 
		Polynom(other.terms.foldLeft(terms)(addTerm))
	scala> def addTerm(terms: Map[Int, Double], term: (Int, Double)): Map[Int, Double] =
		val (exp, coeff) = term; terms + (exp -> (coeff + terms(exp)))

Which version of `addTerm` or `+` is more efficient?

`+`, since add term has to decouple at each iteration and reconstruct.

#### Practice Quiz
1) Select all the statements that are true of the following program:

	val ageOfPeople =
		Map("Alice" -> 7, "Bob" -> 11, "Carol" -> 13)
	val ageOfPeople2 =
		ageOfPeople ++ Map("Ted" -> 10, "Alice" -> 8)
	val ageOfPeople3 =
		ageOfPeople.map((name, age) => (name, age + 1))

	println(ageOfPeople.get("Alice"))  // (1) Some(7)
	println(ageOfPeople.get("Ted"))    // (2) None, get method returns none type if element is not present
	println(ageOfPeople2.get("Alice")) // (3) Some(8)
	println(ageOfPeople3.get("Bob"))   // (4) Some(12)
	println(ageOfPeople3("Ted"))       // (5) throw NoSuchElementException, key access is not allowed for nonexistent elements

2) What collection class should I use to be able to access elements in the order they are inserted into the 
collection and I also want fast random access by index.

	- I should use a 'Vector'
		Correct, A 'Vector' is an indexed sequence, so it stores elements in a given order and it has fast access by index.

## Lecture 6.5 - Putting the Pieces Together
### Task 
phoen keys had *mnemonics* assigned to them 
		
	scala> val mnemonics = Map('2' -> "ABC", '3' -> "DEF", '4' -> "GHI", '5' -> "JKL", 
					'6' -> "MNO", '7' -> "PQRS", '8' -> "TUV", '9' -> "WXYZ")

Assume you are given a dictionary words as a list of words. Design a method encode such that

	scala> encode(phoneNumber)

produces all phrases of words that can serve as *mnemonics* for the phone number.

The phone number "7225247386" should have *mnemonic* **Scala is fun** as one element of the set of solution phrases.

### Outline

	scala> class Coder(words: List[String]):
		val mnemonics = Map('2' -> "ABC", '3' -> "DEF", '4' -> "GHI", '5' -> "JKL", 
				'6' -> "MNO", '7' -> "PQRS", '8' -> "TUV", '9' -> "WXYZ")

		// Maps a letter to the digit it represents
		private val charCode: Map[Char, Char] = for (dig, str) <- mnemonics; ltr <- str yield ltr -> dig
		
		// Maps a word to the digit string it can represent
		privat def wordCode(word: Sting): String = word.toUpperCase.map(charCode)
	
		// Maps a digit string to all words in the dicitonary that represent it
		private val wordsForNum: Map[String, List[String]] = words.groupBy(wordCode).withDefaultValue(Nil)

		// All ways to encode a number as a list of words
		def encode(number: String): Set[List[String]] = ???

### Divide and Conquer

	scala> def encode(number: String): Set[List[String]] =
		if number.isEmpty then Set(Nil) 
		else 
			for splitPoint <- (1 to number.length).toSet
				word <- wordsForNum(number.take(splitPoint))
				rest <- encode(number.drop(splitPoint))
			yield word :: rest

#### Testing It 

	scala> @main def code(numver: String) =
		val coder = Coder(List("Scala", "Python", "Ruby", "C", "Rocks", "socks", "sucks", "words", "pack"))
		coder.encoder(number).map(_.mkString(" "))

As a sample, running `scala code "7225276257"` in the terminal yields `HashSet(Scala rocks, pack C rocks, pack C socks, Scala socks)`

#### Background
This example was taken from a 2000's experiement; For each language, scripting: (Tcl, Python, Perl, Rexx) and 
nonscripting: (Java, C++, C), groups were created to evaluated how efficienty this program could be implemented.
The code size medians were:

- 100 loc for scripting languages
- 200-300 loc for others

Average run times of the scripting languages were also competitive with the others: though fastest solutions were written in C or C++

### Benefits of using Scala
Scala's collections are immutable:
- easy to use: few steps to do the job
- concise: one word replaces a whole loop (map replacing a whole loop)
- safe: type checker is really good at catching errors
- fast: collection ops are tuned, can be parallized
- universal: one vocabulary to work on all kinds of collections (filter & map working on many data types)




















