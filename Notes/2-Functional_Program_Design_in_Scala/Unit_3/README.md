# Type-Directed Programming
## Motivation Example
Seen a type can be infered by the compiler `val x = 42`, where the type of $x$ is `Int`.

This also works with more complex expressions, for instance if we write `val y = x + 1`, 
compiler will also infer type as Int, since `+` is an operation between 2 Int's.

Now we'll see the compiler can also infer a values from a type. Useful idea because when there is exactly on "obvious" value for a type, 
the compiler can find the value and provide it to you.

### Main example
Consider a method sort that takes parameter a `List[Int]` and return another `List[Int]` containing the same elements, but sorted. The implementation of this method..

	def sort(xs: List[Int]): List[Int] =
		...
		... if (x<y) ...
		...

The actual implementation of the sorting algorithm doesn't matter, important part is at some point the method has to compare two elements $x$ and $y$ of the list.
How then can we make this comparision available for a list of anytype? 
As before, create List type polymorphic and provide a `lessThan` subroutine that compares two list elements

	def sort[A](xs: List[A])(lessThan: (A, A) => Boolean): List[A] = 
		...
		... if (lessThan(x,y)) ...
		...

Now we can sort as follows:

	scala> val xs = List(-5,6,3,2,7); val strings = List("apple","pear","orange","pineapple")
	scala> sort(xs)((x,y) => x < y)
	scala> sort(strings)((s1,s2) => s1.compareTo(s2)<0) // Note can use straight less than because of how strings are processed as bits

**Refactoring with Ordering**
The comparison function introduced above is a way to implement an ordering relation- already a type in the standard library called `orderings`.

	package scala.math
	trait Ordering[A] 
		def compare(a1: A, a2: A): Int
		def lt(a1:A, a2: A): Boolean = compare(a1, a2) <= 0
		... 

It has a single abstract method compare, taking two values and returning a positive number if first is "higher" than the second, and negative if not.
lt also provides more convenient operations such as lt, which returns a Boolean indicating whether the first value is lower.
Instead of parameterizing the function lessThan, can refactor with a type of Ordering.

	def sort[A](xs: List[A])(ord: Ordering[A]): List[A] =
		...
		... if (ord.lt(x,y)) ...
		...

With this change, sort method can be called 

	import scala.math.Ordering
	
	sort(xs)(Ordering.Int)
	sort(strings)(Ordering.String)

Note that the symbols `Int` and `String` refere to values not types in this example.
Scala allows for the same symbol to represent values and types- depending on context.
Dependign on contexr, compiler deduces whether a symbol refers to a type or a value.

	object Ordering
		val Int = new Ordering[Int]
			def compare(x: Int, y: Int) = if (x>y) 1 else if (x<y) -1 else 0

	sort(xs)(Ordering.Int)
	sort(strings)(Ordering.String)

Now reducing the Boiler plate, can we eliminate the call `Ordering._`?, ordering argument can be infered from the type of elements to sort

## Implicit Parameters
The first consists in indicating that we want the compiler to supply the ord argument by marking as **implicit**

	def sort[A](xs: List[A])(implicit ord: Ordering[A]): List[A] = ...

Now can call 

	sort(xs)
	sort(strings)

The compiler infers the srgument value based on its expected type. Lets detail the steps the compiler goes through in order to infer implicit parameters.

Consider the expression `sort(xs)`, since xs has type List[Int], the compiler fixes parameter A of sort to Int, so `sort[Int](xs)`.

As a consequence also fixes the expected type of the ord parameter to `Ordering[Int]`. Now the compiler looks for *candiadate definitions* that match the expected type Ordering[Int],
The only matching candidate is Ordering.Int definition, thus sort method becomes, `sort[Int](xs)(Ordering.Int)`. Before we explain how candidate values are defined let's state facts
about Implicits.
- A method can have only on eimplicit parameter list, and it must be the last parameter list given
- At the call site, argumetns of the given clause are ususally left out, though it is possible to explicitly pass them

	sort(xs) // inferred by compiler
	sort(xs)(Ordering.Int.reverse) // Explicit argument

### Candidiates for Implicit Parameters
Where does the compiler look for candidate definitions when it tries to infer an implicit parameter of type T?
Compiler looks for definitions that:
- Have type T
- Are marked implicit
- Are visible at the point of the function call, or are defined in a companion object associated with T

If there is a single (most sepcific) definition, it will be taken as the actual argument in implicit parameters definition, Otherwise an error is reported.

### Implicit Definitions
definition qualified with implicit keyword.

	object Ordering
		implicit val Int: Ordering[Int] = ...

defines an implicit value of type Ordering[Int], named Int. Any val, lazy val, def, or object can be marked implicit.
Finally implicit definitions can take type pasrammeters and implicit parameters(we will learn more about this in a few lessons)

	implicit def orderingPair[A, B](implicit
		orderingA: Oredring[A],
		oreringB: Ordering[B]
	): Ordering[(A,B)] = ...

### Implicit Search Scope
The search for an implicit value of type T frist looks at all the implicit definitions that are visible (inherited, imported, or defined in an enclosing scope)

If compiler does not find an implicit instance matching the queried type T in the lexical scope, it continues searching in the companion objects associated with T. 
There are two concepts: companion objects and types associated with other types.

A companion object is an object witht eh same name as a type. For instance, the object scala.math.Ordering is the companion of the type scala.math.Ordering.

The types associated with a type T are:
- if T has parent types $T_1$ with $T_2$, the union of the parts of $T_1$,...,$T_n$ as well as T itself
- if T is a parameterized  type $S$[$T_1$,...,$T_n$], the union of the parts of $S$ and $T_1$,...,$T_n$
- otherwise just T itself
As example, consider the following type hierarchy:

	trait Foo[A]
	trait Bar[A] extends Foo[A]
	trait Baz[A] extends Bar[A]
	trait X
	trait Y extends X

If an implicit value of type Bar[Y] is required, the compiler will look for implicit definition s in the following companion objects
- Bar, because it is a part of Bar[Y],
- Y, because it is a part of Bar[Y],
- Foo, because it is a parent type of Bar,
- and X, because it is a parent type of Y

### Implicit Search Process
The search process can result into either no candidates found or at least one candidiate ofund, If there is no available implict definition matchin the queried type, 
an error is reported:

	scala> def f(implicit n: Int) = ()
	scala> f
	> error: could not find implicit value for parameter n: Int

On the other hand, if more than one implicit def are elgible, an ambiguity is reported

	scala> implicit val x: Int = 0
	scala> implicit val y: Int = 1
	sscala> def f(implicit n: Int) = ()
	scala> f
	> error: ambiguos implict values:
		both value x of type => Int 
		and value y of type => Int 
		match expected type Int

several implicit definitions matchin the same type dont generate ambiguity if one is more **explict** than another
I.E A is more specific than B if
- type A has more fixed parts
- A is defined in a class or object which is a subclass of teh class defining B
Let's see a few examples of priorities at work.
Which implict definition matches the Int implicit parameter when the following method f is called?

	scala> implicit def universal[A]: A = ???
	scala> implicit def int: Int = ???
	scala> def f(implicit n: Int) = ()
	scala> f
	
because universal takes a type parameter and int doesn't, int has more fixed parts and is considered to be more specific than universal. Thus, there is no ambiguity and
the compiler selects int.

Which implicit definition matches the Int implicit parameter when the follwoing f method is called?

	trait A
		implict val x: Int = 0
	trait B extends A
		implicit val y: Int = 2
		def f(implicit n: Int) =()
		f

since y is defined in a trait thta extends A, y is more specific than x, thus there is no ambiguity between what the compiler should select.

### Context Bounds
Syntacital sugar allows for the omision of the implicit paramter list:

	def printSorted[A: Ordering](as: List[A]): Unit =
		println(sort(as))

type parameter A has one context bound: Ordering. so equivallently

	def printSorted[A](as: List[A])(implicit ev1: Ordering[A]): Unit =
		println(sort(as))

In general

	def f[A: U_1 ...: U_n](ps): R = ...

can expand to 

	def f[A](ps)(implicit ev_1: U_1[A], ..., ev_n:U_n[A]): R = ...

### Implicit Query
At any point in a program, on can **query** an implict value given type by calling the implicity operation

	scala> implicity[Ordering[Int]]
	> Ordering[Int] = scala.math.Ordering$Int$@73564ab0

note that implicity is not a special keyword, it is defined as a library operation

	scala> def implicitly[A](implict value: A): A = value

1) Type-directed programming is a language mechanism that

- infers values from types

2) When the compiler infers an implicit parameter of type T, it searches for possible candidates in

- inherited implicit definitions
- implicit definitions in companion objects of the types associated with the type T
- implicit definitions of outer scopes
- imported implicit definitions

3) What is the output of the following program?

	implicit val n: Int = 42
	def f(implicit x: Int) = x
	println(f)

- 42

4) What is the output of the following program?

	implicit val n: Int = 42
	def f(implicit x: Int) = x
	println(f(0))

- 0

5) How could you change the first line of this program to make it compile?

	val world: String = "World"
	def greet(implicit name: String) = s"Hello, $name!"
	println(greet)

- implicit val world: String = "World"

6) What is the output of the following program?

	trait LowPriorityImplicits {
	  implicit val intOrdering: Ordering[Int] = Ordering.Int
	}
	object Main extends LowPriorityImplicits {
	  implicit val intReverseOrdering: Ordering[Int] = Ordering.Int.reverse
	  def main(args: Array[String]): Unit = {
	    println(List(1, 2, 3).min)
	  }
	}

- 3 

7) Consider the following program:

	trait Show[A] {
	  def apply(a: A): String
	}
	object Show {
	  implicit val showInt: Show[Int] = new Show[Int] {
	    def apply(n: Int): String = s"Int($n)"
	  }
	}

	implicitly[Show[Int]]

def of implicitly

	implicitly[Show[Int]](implicit arg: A): A = arg

- implicitly[Show[Int]](Show.showInt)

8) What is the output of the following program?

	trait Show[A] {
	  def apply(a: A): String
	}
	object Show {
	  implicit val showInt: Show[Int] = new Show[Int] {
	    def apply(n: Int): String = s"Int($n)"
	  }
	}
	def printValue[A: Show](a: A): Unit = {
	  println(implicitly[Show[A]].apply(a))
	}
	printValue(42)

- Int(42)

## Type Classes
Previosuly seen a particular pattern of code combining parameterized types and implicits. Defined parameterized type Ordering[A], 
with implicit instances of that type for concrete types A, and implicit parameter Ordering[A]

	trait Ordering[A]
		def compare(a1: A, a2: A): Int

	object Ordering
		implicit val Int: Ordering[Int]
			new Ordering[Int]
				def compare(x: Int, y: Int) = if (x < y) -1 else if (x > y) 1 else 0

		implicit val String: Ordering[String]
			new Ordering[String]
				def compare(x: String, y: String) = x.compareTo(y) 

	def sort[A: Ordering](xs: List[A]): List[A] = ...

We say that Ordering is sa **type class**.
type classes provide yet another form of polymorphism. Method can be called with lists containing elements of any type A for which
there is an implicit value of type Ordering[A]. When complied, compiler resolves the specific Ordering implementation that matchers the
type of the list elements.

### Retroactive Extension
Type calsses let us add new features t odata types without changing the original definition of these data types. Consider the following
Rational type, modeling a rational number:

	case class Rational(num: Int, denom: Int)

can add the capability "to be compared" to the type Rational by defining an implict instance of type Ordering[Rational]

	object RationalOrdering
		implicit val orderingRational: Ordering[Rational] =
			new Orering[Rational]
				def compare(q: Rational, r: Rational): Int =
					q.num * r.denom - r.num*q.denom

### Laws
We have shown how to implement instances of type class for some specific types (Int, String, and Rational).

New look at the other side: how to use (and reason) about type classes.
Foe example, sort function is kept abstract in terms of Ordering type. If an Ordering instance is incorrect, sort becomes an incorrect 
tool. to prevent this types are often accomplanied by *laws*, which describe properties each instance mmust satisfy.

Instances of `Ordering[A]` must satisfy the following
- inverse: the sign of the result oof comparing x and y must be the inverse of the sign of the result of comparing y and x
- transative: if a value x is lower than y sn thst y id lower than z, x must be also lwoer than z
- consistent: if two values x and y are equal, then the sign of the result of comparing x and z should be the same as comparing y and z

author of a type class should think of such laws and should provide instance implementers to check these laws are satisfied.

### Example fo Type Class: Ring
Let's see how we can define a type class modeling a ring structure.
- assosiative under group: (a + b) + c = a + (b + c)
- communative: a + b = b + a
- 0 is additive identity:  a + 0 = a
- a is the additive inverse of a: a + -a =0
- * is associate: (a*b)*c = a*(b*c)
- 1 is multiplicative idenity: a*1=a
- left distributive: a*(b+c) = a*b+a*c
- right distributive: (b+c)*a = b*a+c*a

	trait Ring[A]
		def plus(x: A, y: A): A
		def mult(x: A, y: A): A def inverse(x: A): A
		def zero: A
		def one: A


	Object Ring
		implicit var ringInt: Ting[Int] = new Ring[Int]
			def plus(x: Int, y: Int): Int = x+y
			def mult(x: Int, y: Int): Int = x*y
			def inverse(x: Int): Int = -x
			def zero: Int = 0
			def one: Int = 1

	def plusAssociativity[A](x: A, y: A, z: A)(implicit ring: Ring[A]): Boolean =
		ring.plus(ring.plus(x , y), z) == ring.plus(a, ring.plus(b, c))

	def plusCommunative[A](x: A, y: A, z: A)(implicit ring: Ring[A]): Boolean =
		ring.plus(x, y) == ring.plus(y, x)

	def additiveIdentity[A](x: A, y: A, z: A)(implicit ring: Ring[A]): Boolean =
		ring.plus(x, 0) == x

	def additiveInverse[A](x: A, y: A, z: A)(implicit ring: Ring[A]): Boolean =
		ring.plus(x, -x) ==0

	def timesAssociative[A](x: A, y: A, z: A)(implicit ring: Ring[A]): Boolean =
		ring.mult(ring.mult(x,y),z) == ring.mult(x,ring.mult(y,z))

	def timesIdentity[A](x: A, y: A, z: A)(implicit ring: Ring[A]): Boolean =
		ring.mult(x,1) == x

	def rightDistributive[A](x: A, y: A, z: A)(implicit ring: Ring[A]): Boolean =
		ring.mult(x,ring.plus(y,z)) == ring.plus(ring.mult(x,y),ring.mult(x,z))

	def leftDistributive[A](x: A, y: A, z: A)(implicit ring: Ring[A]): Boolean =
		ring.mult(ring.plus(y,z),x) == ring.plus(ring.mult(y,x),ring.mult(z,x))

## Conditional Implicit Definitions
In the lesson, we will se that implicit definitions can themselves take implicit parameters.
Let's start with an example. Consider how we order two string values; is "abc" lexicographically before "abd"?
To deduce this, must compare all characters of the String value, element-wise. Since c is before d, "abc" is before "abd".
Can generalize this process to sequences fof any element type A for which there is an implicit Odering[A] instance.
The signature of such an Ordering[List[A]] definiton takes an implicit parameter Ordering[A]: i.e.

	implict def orderingList[A] (implict ord: Ordering[A]): Ordering[List[A]]

a "complete" impelentation is shown below

		implict def orderingZList[A] (implict ord: Ordering[A]): Ordering[List[A]] = 
			new Ordering[List[A]] 
				def compare (xs: List[A], ys: List[A]) =
					(xs, ys) match 
						case (x :: xsTail, y :: ysTail) =>
							val c = ord.compare(x, y)
							iff (c != 0) c else compare(xsTail, ysTail)
						case (Nil, Nil) => 0
					case (Nil, _) => -1
					case (_, Nil) => 1

With this definition can sort a list of list of numbers, 

	scala> val xss = List(List(1,2,3), List(1), List(1,1,3))
	> List[List[Int]] = List(List(1,2,3), List(1), List(1,1,3))
	scala> sort(xss)
	> List[List[Int]] = List(List(1), List(1,1,3), List(1,2,3))

From compilers eyes, how do you sort this type List[List[Int]]

	sort[List[Int]](xss)

Now the compiler looks for an implicti orderirng on a type List[Int]]. Its foun dthat we can find an ordering based on a implict definition of the type Ordering[Int]
So, 

	sort[List[Int]](xss)(ordering(Ordering.Int))

So in this case trhe compiler combiens the deintions fo two implictits, orderingList and ordering.Int, in general an arbitrary amount of implict defintions can 
be combine until the search hits a "terminal" definition. Consider

	implicit def a: A = ...
	implicit def aToB(implicit a: A): B = ...
	implicit def bToC(implicit b: B): C = ...
	implicit def cToD(implicit c: C): D = ... 

Then we can ask the compiler to summon a value of type D:

	implicitly[D]

In pratice the compiler can summon complex fragments of programs such as serializers and deserialziers

### Recursive Implicit Definitions
What happens if we write an implict definition that depends on itself?

	trait X
	implict def loop(implicit x: X): X = x
	implicitly[X]

The compiler detects that it keeps searching for an implict definition of the same type and returns an error

	error: diverging implict expansion for type X
	starting with method loop

** Note:** can write recusrive defintions by making sure they terminate, but outside scope of lesson

### Sort by Multiple Criteria
Consider a situation where we want to compare several movies. Each movie has a title, a rating (in number of "stars") and a duration (in minutes):

	case class Movie(title: String, rating: Int, duration: Int)

	val movies = Seq(
		Movie("Intersetellar", 9, 169),
		Movie("Inglorious Bastratds", 8, 140),
		Movie("Figth Club", 9, 139),
		Movie("Zodiac", 8, 157),
	)

We want to sort movies by rating sirdt an dthen by duration.
To achieve this, a first step is to change our sort function to take parameter the sort criteria in addition to the elements to sort.

	def sort[A, B](elements: Seq[A])(criteria: A => B) (implict
		ord: Ordering[B]
	): Seq[A]

sorting algo remains the same, except instead oc comparing the elements together we compare the criteeria applied to each element. Wuth thuis fuycntion , here is how we can sort movies by title.

	sort(movie)(_.title)

or by rating

	sort(movie)(_.rating)

Each time sort is called, its ordering paramter is inferred by the compiler based on the type of the criteria (String and then Int, in this example).
However, we wanted to sort movies based on multiple criteria. We would liek to sort first by rating and then by duration:

	sort(movies)(movies => (movie.rating, movie.duration))

The type of the criteria is now a tuple, but unfortunately compiler is unable to infer the corresponding ordering parameter. 
need to define how simple orderings can be combined together to get an ordering for multiple criteria. Can define implicit ordering as follows:

	implicit def orderingPar[A, B](implict
		orderingA: Ordering[A]
		orderingB: Ordering[B]
	): Ordering[(A, B)]

complete implicit definition is

	implicit def orderingPair[A, B](implicit
	  orderingA: Ordering[A],
	  orderingB: Ordering[B]
	): Ordering[(A, B)] =
	  new Ordering[(A, B)] {
	    def compare(pair1: (A, B), pair2: (A, B)): Int = {
	      val firstCriteria = orderingA.compare(pair1._1, pair2._1)
	      if (firstCriteria != 0) firstCriteria
	      else orderingB.compare(pair1._2, pair2._2)
	    }
	  }

Now the compiler can inder the ordering for the following call

	sort(movies)(movie => (movie.rating, movie.duration))

Or explicitly

	sort(movies)(movie => (movie.rating, movie.duration))(
		orderingPair(Ordering.Int, Ordering.Int)
	)

Not this is how the sortBy method is defined in the current scala library

## Quiz: Conditional Implicit Definitions
1) Implicit definitions can take implicit parameters?

- true

2) At most two implicit definitions can be chained together.

- false

3) Consider the following program:

	trait Physics {
	  implicit def air: Gaz
	  implicit def condense(implicit gaz: Gaz): Liquid
	  implicit def freeze(implicit liquid: Liquid): Solid

	  implicitly[Solid]
	}

Can you rewrite the last line with the inferred arguments explicitly written? It should look like the following:

	implicitly[Solid](freeze(condense(air)))

# Overwrote Swap - Reference Implicit Conversions-... for more context
## Quiz: Implicit Conversions
1) Check all statements that are True
- Implict Conversions can also take Parameters
- Implicit conversions must take exactly one non-implicit parameter

2) What is the Output of the followign Program

	import scala.language.implictConversions

	case class rational(n: Int, d: Int)

	object Rational 
		implict def fromInt(n: Int) = Rational(n, 1)

	val r: Rational = 42
	println(42)

- Rational(42, 1)

3) Consider the following Program

	import scala.language.implictConversions

	implict class HasIsEven(n: Int)
		def isEven: Boolean = n %2 == 0

	42.isEven

Rewrite the last line to explicitly convert
- new HasIsEven(42).isEven
