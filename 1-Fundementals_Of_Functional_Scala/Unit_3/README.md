# Unit 3 Data and Abstractions

Learned about Objects and Methods, but these are instances of a single class.
Now we'll introduce hierarchy of classes that extend eachother.
The actual methods called will depende on the runtime within the hierarchy.
This concept is known as dynamic binding, an important aspect of Object Oriented Programming.

## Lecture 3.1 - Class Hierarchies
### Abstract Classes

Consider the task of writing a class for sets of integers with the following operations

	scala> abstract class IntSet:
		def include(x: Int): IntSet
		def contains(X: Int): Boolean

IntSet is an **abstract class**.

Abstract classes contain **abstract memebers**; these members are missing their implementation.

Consequently, no direct instance of an abstract class can be created,
for instance an IntSet() call would be illegal.

### Class Extensions

Let's consider implementing sets as binary trees. There are two types of possible trees: 

A tree for the empty set
 
	scala> abstract class IntSet:
		def include(x: Int): IntSet
		def contains(X: Int): Boolean
	scala> class Empty() extends IntSet:
		def include(x: Int): IntSet = NonEmpty(x, Empty(), Empty())
		def contains(x: Int): Boolean = false

and a tree consisting of an integer and two sub-trees.

	scala> class NonEmpty(elem: Int, left: InSet, Right: IntSet) extends IntSet:
		def include(x: Int): IntSet = 
			if x < elem then NonEmpty(elem, left.include(x), right)
			else if x > elem then NonEmpty(elem, left, right.include(x))
			else this
		def contains(x: Int): IntSet = 
			if x < elem then left.contains(x)
			else if x > elem then right.contains(x)
			else true
		
The Set {1, 2, 4, 5} could be represented by the following tree.

Start with 4, notice all numbers on the left subtree are less than 4, 
and all the numbers on the right subtree are greater than 4...

	4
	├── 5
	│   ├── empty
	│   └── empty
	└── 1
	    ├── 2
	    │   ├── empty
	    │   └── empty
	    └── empty

- Consider this implementation of the empty set:
 - The Empty() method contains is always false: empty set doesn't contain an element
 - The Empty() method include creates a NonEmpty set with the included element 
and two empty subtrees

Empty sets are a special case of an IntSet; they're one possible implementation, 
made clear in the extends clause. We say the class Empty extends IntSet, or it conforms 
to the interface of IntSet/implements all the methods defined in IntSet().

- Now consider the nonempty set:
 - The NonEmpty() method contains checks if an IntSet contains element x and compares it
to the root elem. If equal the IntSet contains x, 
else we recursively check the right or left subtrees
 - The NonEmpty() method include creates a NonEmpty set with the elem 
and recursively checks if the right or left subtree includes x- dependent on if its less
or greater than the elem. Else elem and x are same, thus its included and return the IntSet

> {1,2,4,5}.include(3)

	4
	├── 5
	│   ├── empty
	│   └── empty
	└── 1
	    ├── 2
	    │   ├── 3
	    │   │   ├── empty
	    │   │   └── empty
	    │   └── empty
	    └── empty

Asks if 3 is less than 4, greater than 1, greater than 2, empty tree include results in
an empty subtree starting with 3. This new tree includes 4 and now overlays the existing tree;
the right subtree of 4 is left untouch and a tree consisting of an integer and two sub-trees.

**Definitions:** 
- creating new data structures from existing ones, without changing the original is 
known as a **Persitent Data Structures**: Because the old one persists. Standard technique in 
functional programming.
- IntSet is called a **superclass** of Empty and NonEmpty
- Empty and NonEmpty are **subclasses** of IntSet
- The direct or indirect superclass of a class C are called **base classes** of C. The base 
class of NonEmpty include IntSet and Object

In Scala, any user defined class extends another class. When no superclass is given, 
the standard Object in java.lang is assumed. 

### Implementation and Overriding

The definitions of contains and include in the classes Empty and NonEmpty **implement** the
abstract functions in the base trait IntSet.

It's also possible to **redefine** an existing, non-abstract definition 
in a subclass by using override

	scala> abstract class Base:
		def foo = 1
		def bar: Int
	scala> class Sub extends Base:
		override def foo = 2
		def bar = 3

### Object Definitions

In the IntSet example, one could argue there is only a single empty IntSet. 
Thus, its overkill to have the user create multiple instances of Empty.
Can express this case better with an **object definition**

	scala> object Empty() extends IntSet:
		def include(x: Int): IntSet = NonEmpty(x, Empty(), Empty())
		def contains(x: Int): Boolean = false

So this defines a **singleton object** named Empty; 
No other Empty instance can or needs to be created.
Singleton objects are values, so Empty evaluates to itself.

### Companion Objects

An object and a class can have the same name; 
possible since Scala has 2 global namespaces:
One for types and one for values.

Classes live in the type namespace, whereas objects live in the term namespace.

**companions** are classes and objects with the same name in the same sourcefile.

	scala> class IntSet ...
	scala> object IntSet:
		def singleton(x: Int) = NonEmpty(x, Empty, Empty)

	scala> abstract class IntSet:
		def include(x: Int): IntSet
		def contains(X: Int): Boolean
This defines a method to build sets with one element, which can be called as 
	
	scala> IntSet.singleton(elm)

the role of class companion objects is similar to static class definitions in Java-
which are absent in Scala

### Programs

So far have only executed Scala code from the REPL or worksheet, but its also possible to 
create standalone applications in Scala.

Each application contations an object with a **main** method. For example, this is the 
"Hello World!" program in Scala

	scala> object Hello:
		def main(args: Seq[String]): Unit = println("hello world!")
	
After you've recompiled a jar with this new objects we can run this program 
from the command line using

	> scala Hello

This implementaion is similar to Java. Scala has a more convienient way to do this though; 
In Scala a stand-alone application is alternatievely a function that begins 
with the phrase @main and takes command line arguments as parameters.

	scala> @main def birthday(name: String, age: Int) = 
		println(s"Happy Birthday, $name! $age years old already!")
	> scala birthday Peter 11
	Happy Birthday, Petter! 11 years old already!

#### Exercise 3.1.1

Write a method union for forming the union of two sets. 
You should implement the following abstract class.

	scala> abstract class IntSet:
		...
		def union(other: IntSet): IntSet
	scala> class Empty() extends IntSet:
		...
		def union(other: Intset): IntSet = other
	scala> class NonEmpty(elem: Int, left: InSet, Right: IntSet) extends IntSet:
		...
		def union(other: Intset): IntSet = 
			left.union(right).union(other).include(elem)

Union will terminate because it first conbines the whole set, 
then unions elements which are smaller than the original set we started with.
So the left set will get smaller and smaller until we reach the empty set.

A call to union like this is inefficent beacuse we have to first recusively decompose 
the left set to combine its elements.

### Dynamic Binding

Object-Oriented languages (including Scala) implement dynamic method dispatch.
This means the code invoked by a method call depends on the runtime of the object that 
contains the method.	

	scala> Empty.contains(1)
		[1/x][Empty/this]false => false
	scala> (NonEmpty(7, Empty, Empty)).contains(7)
		[7/elem][7/x][NonEmpty(7, Empty, Empty)/this] = 
		if 7 < 7 then left.contains(7) = false = 
		else if 7 > 7 then right.contains(7) = false = 
		else true => true

### Ponder similarities to higher-order functions

Dynamic Dispatch of methods is analogous to calls to higher-order functions:

Can we implement one concept in terms of the other?
- Object in terms of higher-order functions?
- Higher-order functions in terms of objects?

#### Practice Quiz 3.1

1) Abstract classes

	- can have companion objects

		Correct, To define the companion class of an abstract class 
			by using the object keyword followed by the name of the class

	-can be instantiated

	- can be extended

		Correct, you can use the extend keyword to extend an abstract class

	-can extend other classes

		Correct,there are no restrictions on this, abs

## Lecture 3.2 - How classes are organized
### Packages

classes and objects are organized in packages. To place a class or object inside a package, 
use a package clause at the top of your source file. Traditionally store such files in a 
directory like progfun/examples/Hello.scala, though not required.

	scala> package progfun.examples
	scala> object Hello
		...

This would place the object Hello inside the package progfun.examples. 
Now it can be refered to using its fully qualified name.

	> scala progfun.examples.Hello

### Imports

Say we have a class Rational in package week3.You can then use its fully qualified name:

	scala> val r = week3.Rational(1, 2)

Alternatively, you can use an import statement

	scala> import week3.Rational
	scala> val r = Rational(1, 2)

**Import Forms:**

	scala> import week3.Rational // named singular import
	scala> import week3.{Rational, Hello} // named multiple import
	scala> abstract class IntSet:
		def include(x: Int): IntSet
		def contains(X: Int): Boolean
	scala> import week3._ // everything using wildcard

**Automatic Imports:** 
- All members of package scala
- All members of package java.lang
- All members of the singleton object scala.Predef

Examples of types and functions' fully qualified names 

	Int			scala.Int
	Boolean			scala.Boolean
	Object			java.lang.Object
	require			scala.Predef.require
	assert			scala.Predef.assert

Can see the full list at www.scala-lang.org/api/current

### Traits

In Java and Scala a class can only have one superclass.

What if a class has several natural supertypes to which it conforms or 
from which it wants to inherit code?

The solution is traits. A trait is declared like an abstract class, 
just with trait instead of abstract class.

	scala> trait Planar
		def height: Int
		def width: Int
		def surface = height * width

classes, objects and traits can inherit from at most one class but arbitrarily many traits.

	scala> class Square extends Shape, Planar, Movable ...

Traits resemble interfaces in Java, but are more powerful because they can have 
parameters and can contain fields and concrete methods.

### Class Hierarchy

![](Desktop/Scala_Coursera/Unit_3/pictures/type_hierarchy.png)

#### Top Types

At the top of the hierarchy we find:

	Any                    the base type of all types
	                       Methods: [==, !=, equals, hashCode, toString]
	AnyRef                 the base type of all reference types;
	                       Alias of java.lang.Object
	AnyVal                 The base type of all primitive types

#### The Nothing Type

Nothing is at the bottom of Scala's type hierarchy. It's a subtype of every other type.
There is no value of type Nothing. Why is that useful?
- To signal abnormal termination
- As an element type of empty collections (see next section)

#### Exceptions

Scala exception handling is similar to Java's. The expression

	scala> throw Exc

aborts evaluation with the exception Exc. The type of this expression is Nothing

#### Exercise 3.2.1

What is the type of

	scala> if true then 1 else false

This conditional returns either an Int or Boolean, both of which are subtypes of AnyVal

#### Practice Quiz 3.2

1) Consider the following snippet:

		package my
		package pkg.hierarchy

		object Data:
			val course: "Functional Programming Principles"

	What is the fully qualified name of 'course'?

		- 'pkg.hierarchy.my.Data.course'

		- 'my.pkg.hierarchy.Data.course'

			Correct, 'Data' containse 'course' and is the 
				defined in the 'my.pkg.hierarchy' package

		- 'pkg.hierarchy.Data.course'

2) classes, objects and traits can

	- extend at most one class but arbitrary many traits

		Correct, Similarly to Java, in Scala it is possible to have only one 
			superclass but to extend multiple traits (interfaces in Java)

	- extend at most one class but arbitrary many objects

	- extend at most one object but arbitrary many classes

	- extend at most one trait but arbitrary many classes

3) Which statements about the hierarchy of types in Scala are true?

	- 'Nothing' is the base of all types

	- 'AnyRef' is the base of all Java types

		Correct, 'AnyRef' is an alias of 'java.lang.Object' which is the 
			base of all Java types

	- 'AnyVal' is the base of all Java types

	- 'Any' is the subtype of all types

	- 'AnyRef' is the base of all types

	- 'AnyRef' is the subtype of all types

	- 'Nothing' is the subtype of all types

		Correct, 'Nothing' is the base of all types

	- 'Any' is the base of all types

		Correct, 'Any' is at the top of the type hierarchy

	- 'AnyVal' is the base of all types

4) Select the statement that is true about the function 'def f: String = throw Exception()'

	- The compiler rejects this code because 'throw Exception()' does not produce a String

	- The compiler rejects this code because 'Exception' does not extend 'String'

	- The compiler accepts this code because 'Exception' extends 'String'

	- The compiler accepts this code 'throw Exception()' extends 'String'

		Correct, 'throw Exception()' has type 'Nothing' which extends 'String', 
			therefore 'f' is valid

## Lecture 3.3 - Polymorphism 
### Cons-Lists

A fundemental data structure in many functional languages is the immutable linked list.
Its constructed from two building blocks:
- Nil, the empty list
- Cons, a cell containing an element and the remainder of the list

For example the List(1, 2, 3), is constructed by recursively adding elements to the front.

	Cons
	├── Cons
	│   ├── Cons
	│   │   ├── NIL
	│   │   └── 3
	│   └── 2
	└── 1

 List(List(true, false), List(3))

	Cons
	├── Cons
	│   ├── NIL
	│   └── 3
	└── Cons
	    ├── Cons
	    │   ├── NIL
	    │   └── false
	    └── true

### Cons-Lists in Scala

Here's an outline of a class hierarchy that represents lists of integers in this fasion:

	scala> package week3
	scala> trait IntList ...
	scala> class Cons(val head: Int, val tail: IntList) extends IntList ...
	scala> class Nil() extends IntList ...

Now an IntList is either
- an empty list Nil() or
- a list Cons(x, xs) consisting of a head element x and a tail list xs

### Value Parameters

note the abbreviation (val head: Int, val tail: IntList) in the definition of Cons.
This defines parameters and fields of a class. It is equivalent to:

	scala> class Cons(_head: Int, _tail: IntList) IntList:
		val head = _head
		val tail = _tail

where _head and _tail are otherwise unused names.
The problem with this implemenation is that its too narrow, or it only works for Lists of Ints.
We can generalize such an argument with **type parameters**

### Type Parameters

Type parameters are written in square brackets. 
These implementations allow for type abstractions.

	scala> package week3
	scala> trait List[T]
	scala> class Cons[T](val head: T, val tail: List[T]) extends List[T]
	scala> trait Nil[T] extends List[T]

### Complete Definition of List

	scala> trait List[T]:
		def isEmpty: Boolean
		def head: T
		def tail: List[T]
	scala> class Cons[T](val head: T, val tail: List[T]) extends List[T]:
		def isEmpty = false
	scala> class Nil[T] extends List[T]:
		def isEmpty = true
		def head = throw new NoSuchElementException("Nil.head")
		def tail = throw new NoSuchElementException("Nil.tail")

### Generic Functions

Like classes, functions can have type parameters.
For instance, here is a function that creates a list consisting of a single element

	scala> def singleton[T](elem: T) = Cons[T](elem, Nil[T])

We can then write:

	scala> val IntList = singleton[Int](1)
		val IntList: Cons[Int] = Cons@139089a4
	scala> val BoolList = singleton[Boolean](true)
		val IntList: Cons[Boolean] = Cons@38cf3ae1

### Types and Evaluation

Type parameters do not affect evaluation in Scala.
Can assume all type parameters and arguments are removed before evaluating the program.
This property is known as **type erasure**.
Java, Scala, Haskell, ML, OCaml are all langagues that use this property

C++, C#, F# are languages that keep type parameters around at compile time.

### Polymorphism

**Definition:** a **polymorphism** a function whose type comes in many forms.
- the function can be applied to arguments of many types or
- the type can have instances of many types

Two principle forms of polymorphisms:
- subtyping: instances of a subclass can be passed to a base class 
- generics: instances of a fucntion or class are created by type parameterization

#### Exercise 3.3.1

Write a function nth that takes a list and an integer n and 
selects the n'th element of the list.

Elements shouldbe numbered from 0. 
If the index is outside the range from 0 to the list's length minus 1, 
throw an IndexOutOfBoundsException error.
	
	scala> def nth[T](xs: List[T], n: Int): T =. 
	scala> abstract class IntSet:
		def include(x: Int): IntSet
		def contains(X: Int): Boolean
		if xs.isEmpty then throw IndexOutOfBoundsException("List is empty")
		else if n == 0 then xs.head
		else nth[T](xs.tail, n-1)
	scala> nth(cons(1, cons(2, cons(3, nil())), 3)

#### Practice Quiz 3.3

1)Which statement about type parameters is true?

	- can influence evaluation of the program

	- are written inside curly braces

	- need to always be specified when invoking a generic function

	- are a form of polymorphism

		Correct, instances of a function, trait or class of different 
			types can be created by type parameterization of the same type

## Lecture 3.4 - Objects Everywhere
### Pure Object Orientation

A pure object-oriented language is one in which every value is an object.
If the language is based on classes, this means that the type of each value is a class.

In Scala a pure object-oriented language? Seems to be exceptions in primitive types, functions.

### Standard Classes

Conceptually, types such as Int or Boolean do not recieve special treatment in Scala.
They are like the other classes, defined in the package scala.

For reasons of efficiency the Scala compiler represents the values of type scala.Int
by 32-bit integers, and values of type scala.Boolean by Java's Booleans, etc.

### Pure Booleans

The Boolean type maps to JVM's primitive type boolean.
But one could define it as a class from first principles:

	scala> package idealized.scala
	scala> abstract class Boolean extends AnyVal:
		def ifThenElse[T](then: => T, else: => T): T

		def && (x: => Boolean): Boolean = ifThenElse(x, false)
		def || (x: => Boolean): Boolean = ifThenElse(true, x)
		def unary_!: Boolean = ifThenElse(false, true)

		def == (x: Boolean): Boolean = ifThenElse(x, x.unary_!)
		def != (x: Boolean): Boolean = ifThenElse(x.unary_!, x)
		...

### Boolean Constants

here are constants true and false that go with Boolean in idealized scala:

	scala> package idealized.scala
	scala> object true extends Boolean:
		def ifThenElse[T](then: => T, else: => T) = then
	scala> object false extends Boolean:
		def ifThenElse[T](then: => T, else: => T) = else

This method essentially says "if true, then a else b". Revisting other Boolean methods

> true && false = true.&&(false) = true.ifThenElse(false, false) => false

> true || false = true.||(false) = true.ifThenElse(true, false) => true

> true unary_! false = true.unary_!(false) = true.ifThenElse(false, true) => false

> true == false = true.==(false) = true.ifThenElse(false, false.unary_!) => false

> true != false = true.!=(false) = true.ifThenElse(false.unary_!, false) = 
true.ifThenElse(true, false) => true

#### Exercise 3.4.1

Provide an implementation of an implemenation operator ==> for class idealized.scala.Boolean.
If x is true, y is also true, if x is false, y can be arbitrary. 

	scala> extension (x: Boolean):
		def ==> (y: Boolean): Boolean = x.ifThenElse(y, true)

> true ==> true = true.==>(true) = true.ifThenElse(true, true) => true

> true ==> false = true.==>(false) = true.ifThenElse(false, true) => false

> false ==> true = false.==>(true) = false.ifThenElse(true, true) => true

> false ==> false = false.==>(false) = false.ifThenElse(false, true) => false

### The class Int

Here is a partial specification of the class scala.Int

	scala> class Int:
		// Combination Operators, same for -, *, /, %
		def + (that: Double): Double
		def + (that: Float): Float
		def + (that: Long): Long
		def + (that: Int): Int 

		// Shift Operators, >>, >>>
		def << (cnt: Int): Int

		// Bit Mask Operators, same for |, ^
		def & (that: Long): Long
		def & (that: Int): Int

		// Equivalency Operators, same for !=, <, >, <=, >=
		def == (that: Double): Boolean
		def == (that: Float): Boolean
		def == (that: Long): Boolean

Can it be represented as a class from first princicples (i.e. not using primitive ints?)

#### Exercise 3.4.2
 
Provide an implementation of the abstract class Nat that represents non-negative integers.

	scala> abstract class Nat:
		def isZero: Boolean
		def predecessor: Nat
		def successor: Nat = Succ(this)
		def + (that: Nat): Nat
		def - (that: Nat): Nat
		...
	scala> object Zero extents Nat:
		def isZero = true
		def predecessor = throw IndexOutOfBoundsException()
		def + (that: Nat) = that
		def - (that: Nat) = 
			if that.isZero 
			then this 
			else throw IndexOutOfBoundsException()
		...
		override def toString = "Zero"
	scala> class Succ(n: Nat) extends Nat:
		def isZero = false
		def predecessor = n
		def + (that: Nat) = Succ(that + n)
		def - (that: Nat) = 
			if that.isZero
			then this 
			else this - that.predecessor
		...
		override def toString = s"Succ($n)"

## Lecture 3.5-Functions as Objects

Seen that Scala's numeric and Boolean types can be implemented like normal classes, what about functions?

Function values are treated as objects in Scala.
The function type A => B is just an abbrivation for the class scala.Function1[A,B], defined as follows:

	scala> package scala
	scala> trait Function1[A, B]:
		def apply(x: A): B

so functions are an object with the apply methods. There are also traits Function2, Function3, ... 
for functions which take more parameters.

### Expansion of Function Values

An anonymous function such as 

	scala> (x: Int) => x * x

is expanded to:

	scala> new Function1[Int, Int]:
		def apply(x: Int) = x * x

This **anonymous class** can itself be thought of as a block that defines and instantiates a local class:

	scala> {class $anonfun() extends Function1[Int, Int]:
		def apply(x: Int) = x * x
	$anonfun()
	}

### Expansion of Function Calls

A function call, such as $f(a, b)$, where f is a value of some class type, is expanded to

	scala> f.apply(a, b)

So the OO-translation of

	scala> val f = (x: Int) => x * x
	scala> f(7)

would be

	scala> val f = new Function1[Int, Int]:
		def apply(x: Int) = x * x
	scala> f.apply(7)

### Functions and Methods

Note that a method such as

	def f(x: Int): Boolean = ...

If $f$ is used in a place where a Function type is expected, 
it is converted automatically to the function value.

	scala> (x: Int) => f(x)

Expanded:

	scala> new Function1[Int, Boolean]:
		def apply(x: Int) = f(x)

For example take the higher order function $g$

	scala> def g(x: Int => Boolean) = ...
	scala> g(f)

#### Exercise 3.5.1

In package week3, define an:

	scala> abstract class IntSet:
		def include(x: Int): IntSet
		def contains(X: Int): Boolean	
		def union(x: IntSet): IntSet
	scala> object Empty extends IntSet:
		def include(x: Int): IntSet = NonEmpty(x, Empty(), Empty())
		def contains(x: Int): Boolean = false
		def union(other: Intset): IntSet = other
	scala> object NonEmpty(elem: Int, left: InSet, Right: IntSet) extends IntSet:
		def include(x: Int): IntSet = 
			if x < elem then NonEmpty(elem, left.include(x), right)
			else if x > elem then NonEmpty(elem, left, right.include(x))
			else this
		def contains(x: Int): IntSet = 
			if x < elem then left.contains(x)
			else if x > elem then right.contains(x)
			else true
		def union(other: Intset): IntSet = 
			left.union(right).union(other).include(elem)
	scala> object IntSet:
		def apply(): IntSet = Empty
		def apply(x: Int): IntSet = Empty.include(x)
		def apply(x: Int, y: Int): IntSet = Empty.include(x).include(y)

with 3 functions in it so users can create InSets of lengths 0-2 using syntax

	scala> IntSet()		// the empty set
	scala> IntSet(1)	// the set with single element 1
	scala> IntSet(2, 3)	// the set with single elements 2 and 3

#### Practice Quiz 3.5

1) Scala is a pure objected-oriented language

	- true, all values including instances of primitive types and functions are objects in Scala

	- false, functions are not objects

	- false, primitive values such as '0', 'true', '3.1' are not objects

	- false, methods of classes are not objects

		Correct, Every value is an object in Scala
