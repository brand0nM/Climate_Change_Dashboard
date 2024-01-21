# For Expressions and Mondas
For comprehensions are a powerful way to traverse different collections, 
we'll later see that its actually a lot more general than this.
For example it can be used as a querying language for any kind of database.
This is possible because for comprehensions can be represented as higher order functions in Scala.
For comprehensions abstraction can be represented by Monnet's (a topic from category theory).

## Queries with For
The for notation is essentailly equivalent to the common operastions of query languages for databases.

**Definition** the arguments `z <- zs` are generators.

**Example:** Suppose that we have a database books, represented as a list of books

	scala> case class Book(title: String, authors: List[String])

### Mini-Database Querying Example

	scala> val books: List[Book] = List(
		Book("Structure and Interpretation of Computer Programs", 
			List("Abelson", "Harald", "Sussman", "Gerald J.")),
		Book("Introduction To functional Programming" ,
			List("Bird, Ricahrd", "Wadler, Phil")),
		Book("Effective Java", 
			List("Bloch, Joshua")),
		Book("Java Puzzlers", 
			List("Bloch, Joshua", "Gafter, Neal")),
		Book("Programming in Scala", 
			List("Odersky, Martin", "Spoon, Lex", "Venners, Bill"))
	)

### Some Pattern Matching Queries
To find the titles of Books whose author's name is "Bird"

	for
		b <- books
		a <- b.authors
		if a.startsWith("Bird,")
	yield b.title

		List[String] = List("Introduction to Functional Programming")
	
	for
		b <- books
		if b.title.contains("Program")
	yield b.title

		List[String] = List(Structure and Interpretation of Computer Programs, 
			Introduction To functional Programming, Programming in Scala)

**Motivation:** Seems like a good way to traverse database like structures

## Another Query
To find the anmes of all authors who have written at least two books in the present database

	for
		b1 <- books
		b2 <- books
		if b1 != b2
		a1 <- b1.authors
		a2 <- b2.authors
		if a1 == a2
	yield a1

		List[String] = List(Bloch, Joshua, Bloch, Joshua)

	for
		b1 <- books
		b2 <- books
		if b1.title < b2.title
		a1 <- b1.authors
		a2 <- b2.authors
		if a1 == a2
	yield a1

		List[String] = List(Bloch, Joshua)

**Problem:** What happens if an wuthor has published three books

The author is printed three times (A B C) => AB, AC, BC

*Bad Solution:* Remove duplicate authors from result list

	(for
		b1 <- books
		b2 <- books
		if b1.title < b2.title
		a1 <- b1.authors
		a2 <- b2.authors
		if a1 == a2
	yield a1).distinct
	
		List[String] = List(Bloch, Joshua)

Compute the set instead of sequences

	val bookSet = books.toSet
	for
		b1 <- bookSet
		b2 <- bookSet
		if b1 != b2
		a1 <- b1.authors
		a2 <- b2.authors
		if a1 == a2
	yield a1	

		Set[String] = HashSet(Bloch, Joshua)

## Lecture 1.1.5 "Recap": Functions and Pattern Matching
### Case Class Recap
Preferred wauy to define complex data (Java Script Object Notation)
- can map to another JSON object
- can map to a sequence

	{"firstname": "John",
		"lastname": "smith",
		"address": {
			"streetAddress": "21 2nd Street",
			"state": "NY",
			"postalCode": 10021
		},
		"address": [
			{"type": "home", "number": "212 555-1234"}
			{"type": "fax", "number": "646 555-4567"}
		]
	}

#### Representation

	abstract class JSON
	object JSON
		case class Seq(elem: List[JSON]) extends JSON
		case class Obj(bindings: Map[String, JSON]) extends JSON
		case class Num(num: Double) extends JSON
		case class Str(str: String) extends JSON
		case class Bool(b: Boolean) extends JSON
		case object Null extends JSON

#### Example

	val jsData = JSON.Obj(Map(
		"firstname" -> JSON.Str("John"),
		"lastname" -> JSON.Str("smith"),
		"address" -> JSON.Obj(Map(
			"streetAddress" -> JSON.Str("21 2nd Street"),
			"state" -> JSON.Str("NY"),
			"postalCode" -> JSON.Num(10021)
		)),
		"address" -> JSON.Seq(List(
			JSON.Obj(Map("type" -> JSON.Str("home"), "number" -> JSON.Str("212 555-1234"))),
			JSON.Obj(Map("type" -> JSON.Str("fax"), "number" -> JSON.Str("646 555-4567")))
		))
	))

Method to create a string Rep from JSON data

	def show(json: JSON): String = json match
		case JSON.Seq(elem) =>
			elems.map(show).mkString("[",", ","]")
		case JSON.Obj(bindings) =>
			val assocs = bindings.map(
			(key, value) => s"${inQuotes(key)}: ${show(value)}")
			assocs.mkString("{",",\n","}")
		case JSON.Num(num) => num.toString
		case JSON.Str(str) => inQuotes(str)
		case JSON.Bool(b) => b.toString
		case JSON.Null => "null"
	def inQuotes(str: String): String = "\"" + str + "\""
	
### Collections
Scala has a rich hierarchy oof collection classes.
- Iterable
 - Seq
  - List
 - Set
 - Map

**Core Methods:**
- map

	extension[T](xs: List[T])
		def map[U](f: T => U): List[U] = xs match
			case x :: xs1 => f(x) :: xs1.map(f)
			case Nil => Nil

- flatMap

	def flatMap[U](f: T => List[U]): List[U] = xs match
		case x :: xs1 => f(x) ++ xs1.flatMap(f)
		case Nil => Nil

- filter

	def filter(f: T => Boolean): List[T] = xs match
		case x :: xs1 => if f(x) then x :: xs1.filter(f) else xs1.filter(f)
		case Nil => Nil

- foldLeft
- foldRight

In practice the implementation of these methods are a little different 
- so they can be applied to aribitrary collections, not just lists.
- make them tail recursive on lists

#### For-Expressions
Used to simplify core methods `map`, `flatMap`, `filter`

	(1 until n)(i =>
		(1 until i) filter (j => isPrime(i+j)) map
			(j=>(i,j)))

	for
		i <- 1 until n
		j <- 1 until i
		if isPrime(i+j)
	yield (i,j)

#### For- Expressions with Pattern Matching

	def binding(x: JSON): List[(String, JSON)] = x match
		case JSON.Obj(bindings) => bindings.toList
		case _ => Nil
	for 
		case("phoneNumbers", JSON.Seq(numberInfos)) <- bindings(jsData)
		numberInfo <- numberInfos
		case("number", JSON.Str(number)) <- bindings(numberInfo)
		if number.startWith("212")
	yield number

If the pattern starts with case, the sequence is filtered so that only elements matching the pattern are retained

## Lecture 1.2 - Translation of For
### For-Expressions and Higher-Order Functions
The sytax for is closely related to the higher-order functions `map`, `flatMap`, and `filter`.

First of all, these functions can be defined in terms of for:

	scala> def mapFun[T, U](xs: List[T], f: T => U): List[U] =
		for x <- xs yield f(x)
	scala> def flatMap[T, U](xs: List[T], f: T => Iterable[U]): List[U] =
		for x <- xs; y <- f(x) yield y
	scala> def filter[T](xs: List[T], p: T => Boolean): List[T] =
		for x <- xs if p(x) yield x

### Translation of For
In realtiy the `Scala` compiler expresses for-expressions in terms of `map`, `flatMap` and a 
lazy variant of `filter`.

1) A simple for-expression, `for x <- e1 yield e2`, is translated to `e1.map(x => e2)`

2) A for-expression, `for x <- e1 if f; s yield e2`, where `f` is a `filter` and `s` is a (potentially empty)
sequence of generators and filters, is translated to: `for x <- e1.withFilter(x => f); s yield e2`

You can think of `withFilter` as a variant of `filter` that does not produce an intermediate list, 
but instead applies the following `map` or `flatMap` function application only to those elements that 
passed the test.

3) A for-expression `for x <- e1; y <- e2; s yield e3`, where `s` is a (potentially empty) 
sequence of generators and filters, is translated into `e1.flatMap(x => for y <- e2; s yield e3)`,
(and the translation continues with the new expression)

**Example:** Take the for-expression that computed the pairs hose sumis prime

	for
		i <- (1 until n)
		j <- (1 until i)
		if isPrime(i + j)
	yield (i, j)

Applying the translationm scheme to this expression gives

	(1 until n).flatMap(i => 
		(1 until i).withFilter(j => 
			isPrime(i + j)).map(j => 
				(i, j)))

#### Exercise 1.2.1
Translate

	for
		b <- books
		a <- b.authors 
		if a.startsWith("Bird")
	yield b.title

higher-order implementation

	books.flatMap(book => 
		book.author.withFilter(author => 
			author.startsWith("Bird")).map(book => 
				book.title))

### Generalization of for
Interestingly, the translation of for is not limited to lists or sequences, or even collections;
It is based soley on the presence of the methods `map`, `flatMap`, and `withFilter`.

This lets you use the for syntax for your own types as well - you must only define 
`map`, `flatMap`, and `withFilter` for these types.
There are many types for which this is useful: array, iterators, databases, optional values, parsers, etc.

### For and Databases
For exapmle, `books` might might not be a list, but a database stored on some server.

As long as the client interface to the database defines the methods `map`, `flatMap` and `withFilter`, 
we can use the for syntax for querying the database.

This is the basis of data base connection fameworks such as *Slick* or *Quill*, as well as 
big data platforms such as *Spark*

## Lecture 1.3 - Functional Random Generators
### Other Uses of For-Expressions

**Question:** Are for-expressions tied to collection-like things such as lists, sets, or databases?

**Answer:** No! All that is required is some interpretation of `map`, `flatMap` and `withFilter`

There are many domains outside collections that afford such an interpretation

**Example:** random value generators

### Random Values
You know about random numbers:

	scala> val rand = java.util.Random()
		rand.nextInt()

**Question:** What is a systematic way to get random values for other domains, such as
- booleans, strings, pairs and tuples, lists, sets, trees

### Generators
Let's define a trait `Generator[T]` that generates random values of type `T`

	scala> trait Generator[+T]:
		def generate(): T

Some instances:

	scala> val integers = new Genrator[Int]:
		val rand = java.util.Random()
		def generate() = rand.nextInt()

	scala> val booleans = new Generator[Boolean]:
		def generate() = integers.generate() > 0

	scala> val pairs = new Generator[(Int, Int)]:
		def generate() = (integers.generate(), integers.generate())

	scala> pairs.generate() 

		(Int, Int) = (-2086394054, 1760038845)
		pairs(integers, booleans).generate()

### Streamlining it
Can we avoid the `new Generator` ... boilerplate?
Ideally, we would like to write:

	scala> val booleans = for x <- integers yield x>0
	scala> def pairs[T, U](t: Generator[T], u: Generator[U]) = 
		for x<- t; y <- u yield (x, y)

What does this expand to?

	scala> def pairs[T, U](t: Generator[T], Generator[U]) =
		t.flatMap(x => u.map(y => (x, y)))

Need `map` and `flatMap` for that

#### Generator with map and flatMap
Here's a more convenient version of Generator:

	scala> trait Generator[+T]:
		def generate(): T
	scala> extension [T, S](g: Generator[T])
		def map(f: T => S) = new Generator[S]:
			def generate() =f(g.generate())
	scala> def flatMap(f: T => Generator[S]) = new Generator[S]:
		def generate() = f(g.generate()).generate()

We can also implement `map` and `flatMap` as methods of class `Generator`:

	scala> def map[S](s: T => S)  = new Generator[S]:
		def generatr() = f(Generator.this.generate())
	scala> def flatMap[S](f: T => Generator[S]) = new Generator[S]:
		def generate() = f(Generator.this.generate()).generate()

Note the use of `Generator.this` to the refer to the this of the "outer" object of class `Generator`.

### The booleans Generator
Wht does this definition resolve to?

	scala> val booleans = for x <- integers yield x>0
	scala> val booleans = integers.map(x => x>0)
	scala> val booleans = new Generate[Boolean]:
		def generate() = ((x: Int) => x>0)(integers.generate())
	scala> val booleans = new Generator[Boolean]:
		def generate() = integers.generate() > 0

### The Pairs Generator

	scala> def pairs[T, U](t: Generator[T], u: Generator[U]) = t.flatMap(
		x => u.map(y => (x, y)))
	scala> def pairs[T, U](t: Generator[T], u: Generator[U]) = t.flatMap(
		x => new Generator[(T, U)]{ def generate() = (x, u.generate()) })
	scala> def pairs[T, U](t: Generator[T], u: Generator[U]) = new Generator[(T, U)]:
		def generate() = (new Generator[(T, U)] {
			def generate() = (t.generate(), u.generate())
		}).generate()
	scala> def pairs[T, U](t: Generator[T], u: Generator[U]) = new Generator[(T, U)]:
		def generate() = (t.generate(), u.generate())

### Generator Examples

	scala> def single[T](x: T): Generator[T] = new Generator[T]:
		def generate() = x
	scala> def range(lo: Int, hi: Int): Generator[Int] = 
		for x <- integers yield lo + x.abs % (hi - lo)	
	scala> def oneOf[T](xs: T*): Generator[T] = 
		for idx <- range(0, xs.length) yield xs(idx)
	scala> val choice = oneOf("red","green","blue")
	scala> choice.generate()

		String = "blue" => "green" => ....

### A list Generator
A list is either an empty list or non-empty list

	scala> def lists: Generator[List[Int]]
		for
			isEmpty <- booleans
			list <- if isEmpty then eptyLists else nonEmptyLists
		yield list
	scala> def emptyLists = single(Nil)
	scala> def nonEmptyLists =
		for
			head <- integers
			tail <- lists
		yield head :: tail
	scala> lists.generate() 
		
		List[Int] = List()

...

	scala> def lists: Generator[List[Int]]
		for
			kind <- range(0, 5)
			list <- if kind ==0 then emptyLists else nonEmptyLists
		yield list

### A Tree Generator
Can you implement a generator that creates random `Tree` objects?

	enum Tree:
		case Inner(left: Tree, right: Tree)
		case Leaf(x: Int)
	def trees: Generator[Tree] = 
		for
			isLeaf <- booleans
			tree <- if isLeaf then leaf else inners
		yield tree
	def leafs = for x<- integers yield Tree.Leaf(x)
	def inners = for x<-trees; y<-trees yield Tree.Inner(x, y)
	trees.generate()

### Application: Random Testing
You know about `unit` tests:
- Come up with some test imputs to program fucntions and a **postcondition**
- The postcondition is a property of the expected result
- Verify that the program satisfies the postcondition

**Question:** Can we do without the test input? Yes, generate the test inputs

### Random Test Function
Using generators, we can write a random etst function:

	scala> def test[T](g: Generator[T], numTimes: Int = 100)
			(test: T => Boolean): Unit =
			for i <- 0 until numTimes do
				val value = g.generate()
				assert(test(value), "test failed for "+value)
			println("passed "+numTimes+" tests")

Example usage:

	scala> test(pairs(lists, lists)) {
		(xs, ys) => (xs ++ ys).length > xs.length
	}

No, since pairs can flatMaps to tuple of same length, replace > -> >=

### ScalaCheck
Shift in viewpoint: Instead of writing tests, write **properties** that are assumed to hold.
This idea is implemented in the `ScalaCheck` tool.

	forAll { (l1: List[Int], l2: List[Int]) =>
		l1.size + l2.size == (l1 ++ l2).size
	}

It can be used either stand-alone or as part of Scala Test.

## Lecture 1.4 - Monads
Data structures with `map` and `flatMap` seem to be quite common. 
In fact there;'s a name that describes this class of a data structures together with some algebraic laws
that they should have. They are called **monads**.

### What is a Monad?
A monad `M` is a parametric type M[T] with two operations, flatMap and unit, that ahve to satisfy some laws.

	scala> extension [T, U](m: M[T])
		def flatMap(f: T => M[U]): M[U]

	scala> def unit[T](x: T): M[T]

In the literature, `flatMap` is also called `bind`. It can be an extension method, or be defined as a regular 
method in the monad class `M`.

**Examples of Monads**
- List is a monad with unit(x) = List(x)
- Set is monad with unit(x) = Set(x)
- Option is a monad with unit(x) = Some(x)
- Generator is a monad with unit(x) = single(x)

### Monads and map
map can be defined for every monad as a combination of flatMap and unit:

	scala> m.map(f) == m.flatMap(x => unit(f(x))) == m.flatMap(f andThen unit)

**Note** andThen is defiend function composition in the standard library.

	scala> extension [A, B, C](f: A => B)
		infix def andThen(g: B => C): A => C =
			x => g(f(x))

### Monad Laws
To qualify as a monad a type has to satify three properties

1) Associativity

	m.flatMap(f).flatMap(g) == m.flatMap(f(_).flatMap(g))

2) Left Unit

	unit(x).flatMap(f) == f(x)

3) Right Unit

	m.flatMap(unit) == m

### Checking the Monad Laws
Let's check the monad law for `Option`.
Here's `flatMap` for `Option`:

	scala> extension [T](xo: Option[+T])
		def flatMap[U](f: T => Option[U]): Option[U] = xo match
			case Some(x) => f(x)
			case None => None 

**Check the Left Unit Law**: Need to show: `Some(x).flatMap(f) == f(x)` 

	Some(x).flatMap(f)
	== Some(x) match
		case Some(x) => f(x)
		case None => None
	== f(x)

**Check the Right Unit Law**: Need to show: `Some(x).flatMap(Some) == opt` 

	opt.flatMap(Some)
	== opt match
		case Some(x) => Some(x)
		case None => None
	== opt

**Check Associativity**: Need to show: `opt.flatMap(f).flatMap(g) == opt.flatMap(f(_).flatMap(g))`

	opt.flatMap(f).flatMap(g)
	== (opt match {case Some(x) => f(x) case None => None})
		match {case Some(y) => g(y) case None => None}
	== opt match 
		case Some(x) => 
			f(x) match { case Some(y) => g(y) case None => None})
		case None =>
			None match {case Some(y) => g(y) case None => None}
	== opt match 
		case Some(x) => 
			f(x) match { case Some(y) => g(y) case None => None})
		case None => None
	== opt match 
		case Some(x) => f(x).flatMap(g)
		case None => None
	== opt.flatMap(x => f(x).flatMap(g))
	== opt.flatMap(f(_).flatMap(g))

### Significance of Laws for For-Expressions
We have seen that monad-typed expressions are typically written as for expressions.
What is the significance of the laws with respect to this?

1) Associativity says essentially that one can "inline" nested for expressions:

	for
		y <- for x <- m; y <- f(x) yield y
		z <- g(y)
	yield z

	== for x <- m; y <- f(x); z <- g(y) yield z

2) Right unit says

		for x <- m yield x == m

3) Left unit does not have an analogue for for-expressions

## Lecture 1.5 - Exceptional Monads
### Exceptions
Exceptions in Scala are defined similarly as in Java

An exception class is any subclass of `java.l;ang.Throwable`, which has itself subclasses 
`java.lang.Exception` and `java.lang.Error`. Values of exception classes can be thrown 

	class BadInput(msg: String) extends Exception(msg)
	throw BadInput("missing data")

A thrown exception terminates computation, if it is not handled with a try/catch

### Handling Exceptions with try/catch
A `try`/`catch` expression consitist of a `body` and one or more `handlers`

	scala> def validatedInput(): String =
		try getInput()
		catch
			case BadInput(msg) => println(msg); validatedInput()
			case ex: Exception => println("fatal error"); throw ex

### try/catch Expressions
An exception is caught by the closet enclosing `catch` handler that matches its type.

This can be formalized with a variant of the substitution model. Roughly:

	try e[throw ex] catch case x: Exc => handler
	[x:=ex]handler

Here, ex: Exc and e is some arbitrary "evaluation context"
- that `throw ex` as next instruction to execute and
- that does not contain a more deeply nested handler that matches `ex`

### Critique of try/catch
Exception are low-overhead way for handlign abnormal conditions.

But there have also some shortcomings.
- They don't show up in the types of function shtat `throw` them. (in `Scala`, in `Java` they do show up in
 `throws` clauses but that has its own set of downsides)
- They don't work in parallel computations where we want to communicate an exception from one thread to another

So in some situations it makes sense to see an exception as a normal function result value, instead of something `special`

This idea is implemented in the `scala.util.Try` type.

### Handling Exceptions with try/catch
A try/catch expression consists of a *body* and one or more *handlers*

	scala> def validatedInput(): String =
		try getInput()
		catch
			case BadInput(msg) => println(msg); validatedInput()
			case ex: Exception => println("fatal error"); throw ex

### Critique of try/catch
Exceptions are low-overhead way for handling abnormal conditions.

But there have also some shortcomings.
- They don't show up in the types of functions that throw trhem. (in `Scala`, in `Java` they do show up in `throws`
clauses but that has its own set of downsides)
- They don't work in parallel computations where we want to communicate an exception from one thread to another

So in some situations it makes sense to see an exception as a normal fucntion result value, instead of something special.
This idea is implemented in the `scala.util.Try` type

### Handling Exceptions with the Try Type
`Try` resembles `Option`, but instead of `Some/None` there is a Success case with a value and a Failure case that 
contains an exception:

	scala> abstract class Try[+T]
	scala> case class Success[+T](x: T) extends Try[T]
	scala> case class Failure(ex: Exception) extends Try[Nothing]

A primary use of `Try` is as a means of passing between threads and processes results of computations that can
fail with an exception

### Creating a Try
You can wrap up an arbitrary computation in a Try. `Try(expr)`

Here's an implementation of `Try.apply`:

	scala> import scala.util.control.NonFatal
	scala> object Try:
		def apply[T](expr: => T): Try[T] =
			try Success(expr)
			catch case NonFatal(ex) => Failure(ex)

`NonFatal` matches all exceptions that allow to continue the program.

### Composing Try
Just like with `Option`, Try-valued computations can be composed in for-expressions

	for
		x <- computeX
		y <- computeY
	yield f(x, y)

It `computeX` and `computeY` succeed with results `Success(x)` and `Success(y)`, this will return `Success(f(x, y))`.
If either computation fails with an exception ex, this will return `Failure(ex)`.

### Definition of flatMap and map on Try

	extension [T](xt: Try[T]):
		def flatMap[U](f: T => Try[U]): Try[U] = xt match
			case Success(x) => try f(x) catch case NonFatal(ex) => Failure(ex)
			case fail: Failure => fail
		def map[U](f: T => U): Try[U] = xt match
			case Success(x) => Try(f(x))
			case fail: Failure => fail
		
So, for a Try value t, 

	t.map(f) == t.flatMap(x => Try(f(x)))
		== t.flatMap(f andThen Try)

#### Exercise
It looks like `Try` might be a monad, with unit = Try

	object Try:
		def apply[T](expr: => T): Try[T] =
			try Success(expr)
			catch case NonFatal(ex) => Failure(ex)

The left unit law fails, will never throw a non-fatal exception whereas right will throw any exception thrown by `expr` or `f`
`Try(expr).flatMap(g) != f(expr)`

Hence, Try trades one monad law for another law which is more useful in this context:
*An expression composed from 'Try', 'map', 'flatMap' will never throw a non-fatal exception*

Call this the "bullet-proof" principle

### Conclusion
We have seen that for-expressions are useful not only for collections.
Many other types also define `map`, `flatMap`, and `withFilter` operations and with them for-expressions

**EX:** Generator, Option, Try

Many of the types defining `flatMap` are monads.
(If they also define `withFilter`, they are called "monads with zero").
The three monad laws give useful guidance in the design of library APIs



















































