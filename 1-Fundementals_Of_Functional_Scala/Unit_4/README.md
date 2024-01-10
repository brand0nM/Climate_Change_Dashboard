# Types and Pattern Matching
Shift focus from data construction to data decomposition. 
What are good ways to find out what's in the data and good ways to act on that knowledge. 
Look at **pure data structures** that don't encapsulate any behavior, but exist only for being decomposed.

## Lecture 4.1 - Decomposition
By example, lets a small interpreter for arithmetic expressions.
For simplicity we'll restrict ourselves to numbers and additions.

Expressions can be represented as a  class hierarchy, with a base trait `Expr` and 
two subclasses `Number` and `Sum`.
To manage this expression, its necessary to know the expressions "shape" and its "components.

	scala> trait Expr:
		def isNumber: Boolean
		def isSum: Boolean
		def numValue: Int
		def leftOp: Expr
		def rightOp: Expr
	scala> class Number(n: Int) extends Expr:
		def isNumber = true
		def isSum = false
		def numValue = n
		def leftOp = throw Error("Number.leftOp")
		def rightOp = throw Error("Number.rightOp")
	scala> class Sum(e1: Expr, e2: Expr) extends Expr:
		def isNumber = false
		def isSum = true
		def numValue = throw Error("Sum.numValue")
		def leftOp = e1
		def rightOp = e2
	scala> def eval(e: Expr): Int = 
		if e.isNumber then e.numValue
		else if e.isSum then eval(e.leftOp) + eval(e.rightOp)
		else throw Error("Unknown expression "+ e)
		
**Note** Writing classes and accessor fucntions like this becomes tedious though.
Additionally, there is no guarentee you'll use the right accessor function 
(each class might throw an error, so Error + something can not resolve;
Or there isnt a compile time guarentee).

### Adding New Forms of Expressions
So what happends if you want to add new expression forms, say

	scala> class Prod(e1, Expr, e2: Expr) extends Expr	// e1*e2
	scala> class Var(x: String) extends Expr		// Variable 'x'
	
**Consequence**, you'll need to add methods for classification and access to all classes defined above

#### Exercise 4.1.1
To integrate `Prod` and `Var` into the hierarchy, how many new method definitions do you need?
(Including method definitions in `Prod` and `Var` themselves, 
but not counting methods that were already given on the slides)

	scala> trait Expr:
		...
		isVar: Boolean
		isProd: Boolean
		varName: String
		(possibly depending on count) (leftOp, rightOp)
	scala> class Number(n: Int) extends Expr:
		... 3-5 new methods
	scala> class Sum(e1: Expr, e2: Expr) extends Expr: 
		... 3-5 new methods
	scala> class Prod(e1, Expr, e2: Expr) extends Expr:
		... 8-10 methods
	scala> class Var(x: String) extends Expr
		... 8-10 methods
	
The correct answer is 25:35? So this implementation is not very scalable

### Non-Solution: Type Tests and Type Casts
This implementation is dicouraged in Scala because there exist a better solution.

Scala will allow you to define methods in class Any:

	scala> def isInstanceOf[T]: Boolean	// checks whether object's type conforms
	scala> def asInstanceOf[T]: T		// treats this object as an instance of type 'T'
						// throws 'ClassCastException' if it isn't 

Correspond to Java's type tests and casts

	Scala				Java
	x.isInstanceOf[T]		x instanceof T
	x.asInstanceOf[T]		(T) x

This solution is quite ugly and prone to errors.

	scala> def eval(e: Expr): Int = 
		if e.isInstanceOf[Number] then 
			e.isInstanceOf[Number]	
		if e.isInstanceOf[Sum] then
			eval(e.isInstanceOf[Sum].leftOp) +
			eval(e.isInstanceOf[Sum].rightOp)
		else throw Error("Unknown expression "+ e)
	
### Solution 1: Object-Oriented Decomposition
For example, suppose all you want to do is *evaluate* expressions.
One can define:

	scala> trait Expr
		def eval: Int
	scala> class Number(n: Int) extends Expr:
		def eval: Int = n
	scala> class Sum(e: Expr, e2: Expr) extends Expr:
		def eval: Int = e1.eval + e2.eval

What if you'd like to dispaly these expressions?
You'd have to define a new method `show` in the base class each subclass

Take away, this approach works, but still blows up with exponentially many methods

#### Assesment of OO Decomposition

- mixes data with operations on the data
- This can be the right thing if there's a need for encapsulation and data abstraction.
- On the other hand it increases complexity(*) and adds new dependancies to classes
- Makes it easy to add new kinds of data but hard to add new kinds of operations

(*) In the literal sense of the word: complex = plaited, woven together

Thus, complexity arises from mixing several things together

#### Limitations of OO Decomposition
This only works well if operations are on a *single* object.
What if you want to simplify the expression using the rule: `a*b+a*c -> a*(b+c)`

The problem is this is a non-local simplification, it cant be encapsulated in the method of a single object.

Hence, back at square 1; you'll need test and access methods for all the different subclasses.

#### Practice Quiz 4.1
1) Object-oriented decomposition mixes

	- type tests and casts
	- classification and access methods
	- data and operations on the data

	Correct, In object-oriented classes, traits and objects contain 
		both data and the methods to operate on it.

## Lecture 4.2 - Pattern Matching
### Solution 2: Functional Decomposition with Pattern Matching
Observation: the sole purpose of test and accessor functions is to reverse the construction process
- Which subclass was used
- What were the arguments of the constructor?

**Note** Process is so common many functional languages- including Scala- automate it.

### Case Classes
A *case class* definition is similar to normal class definition, 
except it is preceded by the modifier "case". For example

	scala> trait Expr
	scala> case class Number(n: Int) extends Expr:
	scala> case class Sum(e1: Expr, e2: Expr) extends Expr:

Like before we declare one trai `Expr` with two subclasses, `Number` and `Sum`;
however, now these instances are declared as empty.

### Pattern Matching
*Pattern Matching* is a generalization of `switch` from C/Java to class hierarchies.
Expressed in Scala using the keyword `match`.

	scala> def eval(e: Expr): Int = e match
		case Number(n) => n
		case Sum(e1, e2) => eval(e1) + eval(e2)
	
### Match Syntax
**Rules:**
- match is preceded by a selector expression and is followed by a sequence of **case**,
`pat => expr`.
- Each case associates an **expression** expr with a **pattern** pat
- A `MatchError` excpection is thrown if no pattern matches the value of the selector

### Forms of Patterns
Patterns are constructed from:
- constructors e.g. Number, Sum,
- variables e.g. n, e1, e2
- wildcard partterns _
- constants e.g. 1, true
- type tests, e.g. n: Number

**Notes:**
Variables always begin with a lowercase letter.

Names of constants begin with a capital letter,
with the exception of the reversed words null, true and false.

The same variable name can only appear once in a pattern. So, Sum(x, x) is not a legal pattern.

### Evaluating Match Expressions
An expression of the form

> e match {case $p_1$ => $e_1$ ... case $p_n$ => $e_n$}

matches the value of the selector $e$ with the patterns $p_1, ..., p_n$
in the order in which they are written.

The whole match expression is rewritten to the right-hand side of the first case
where the pattern amtches the selector $e$

References to pattern variables are replaced by the corresponding parts in the selector.

### What Do Patterns Match?
- A constructor pattern $C(p_1, ..., p_n)$ matches all values of type $C$ (or a subtype)
that have been constructed with arguments matching the patterns $p_1, ..., p_n$
- A variable pattern $x$ matches any value and **binds** the name of the variable to this value
- A constant pattern $c$ matches values that are equal to $c$ (in the sense of ==)

**Examples:**

The sum evaluation is as follows,

	scala> eval(Sum(Number(1), Number(2)))
	scala> eval(Sum(Number(1), Number(2))) match
		case Number(n) => n
		case Sum(e1, e2) => eval(e1) + eval(e2)
	
		eval(Number(1)) + eval(Number(2))

The number evaluation is as follows,

	scala> Number(1) match
		case Number(n) => n
		case Sum(e1, e2) => eval(e1) + eval(e2)
		
		+ eval(Number(2)) => 1 + eval(Number(2)) => 3

Of course, it's also possible to define the evaluation fucntion as a method of the base trait

	scala> trait Expr:
		def eval: Int = this match
			case Number(n) => n
			case Sum(e1, e2) => e1.eval + e2.eval

Excercise write a function show that uses pattern matching to return the 
representation of a given expression as a string.

	scala> def show(e: Expr): String = e match
		case Number(n) => n.toString
		case Sum(e1, e2) => e1.show + " + " + e2.show

#### Exercise (Optional, Harder)
Add case classes `Var` for variables $x$ and `Prod` for products $x * y$ as discussed previously.

	scala> case class Var(x: String) extends Expr:
	scala> case class Prod(x: Number, y: Number) extends Expr:
	
Change your show function so that it also deals with products.

	scala> def show(e: Expr): String = e match
		...
		case Var(n) => n
		case Prod(x, y) => x.showP + " * " + y.showP

	scala> def showP(e: Expr): String = e match
		case e: Sum => "(" + e.show + ")"
		case _ => e.show

Pay attention you get operator precedence right but use as few parentheses as possible.

**Examples:**
	Sum(Prod(2, Var("x")), Var("y")) => "2 * x + y"
	Sum(Prod(2, Var("x")), Var("y")) => "(2 + x) * y"

#### Practice Quiz 4.2
1) The main advantages of using case classes instead of classes are

	- Case classes can extend traits while classes cannot

	- You can use type tests in 'match' expressions to evaluate different 
		expressions according to the runtime type of the value you “match” on

	- The compiler creates accessors for the attributes

		Correct, The compiler creates a '.field_i' accessor for each 'field_i' of the case class

	-We can use pattern matching to access the arguments of the constructor that created an object

		Correct, We can get a reference to the arguments of the constructor. For example in
		def eval(e: Expr): Int = e match
			case Number(n) => n
			case Sum(e1, e2) => eval(e1) + eval(e2)

			'n' is a reference to the value passed to create 'e' when 'e' is a 'Number'

Question 2
Select all the kinds of patterns that can be used in Scala in 'match' expressions.
	- type tests

		Correct, A typed pattern matches if the value is an instance of the given type:
		def isNumber(e: Expr): Boolean = e match
			case n: Number => true
			case _ => false

			the first case matches only when the type of 'e' is 'Number'

	- constructors of regular classes

	- Wildcard patterns

		Correct, The wildcard pattern matches all the cases. For example, in

		def isNumberMessage(e: Expr): String = e match
			case Number(n) => "This is a number"
			case _ => "This is not a number"

			the second case matches when 'e' is anything but a number.

	- variables

		Correct, Variable patterns are often used as fallbacks. For example, in

		def isNumberMessage(e: Expr): String = e match
			case Number(n) => "This is a number"
			case v => "This is not a number"

			'v' is a variable pattern

	- case class constructors

		Correct, In the lecture, we used constructor patterns such as 'case Number(n)'

	- literal constants

		Correct, A literal pattern matches only if the value is equal to the literal value:

		def isNumber2(n: Int): Boolean = e match
			case 2 => true
			case _ => false

			the first case matches only when 'n == 2'

## Lecture 4.3 - Lists
The list is a fundemental data structure in functional programming.

A list having $x_1, .., x_n$ as elements is written List($x_1, ..., x_n$)

**Example:**
	
	scala> val fruits = List("Apples","Pears","Strawberries")
	scala> val diag3 = List(List(1,0,0), List(0,1,0), List(0,0,1))
	scala> empty = List()

Biggest difference between list and arrays in Scala is that 
- Lists are immutable- its elements can't be changed
- Lists are recursive, while arrays are flat 

![]("Desktop/Scala_Coursera/Unit_4/Pictures/List_Const_Flow.pdf")

### Constructors of Lists
All lists are constructed from:
- the empty list Nil, and
- the  construction operation :: (pronounced cons). x:: xs gives the List(x, xs)

> fruit = "Apples" :: ("Pears :: "Strawberries" :: Nil)

### Right Associativity
Convention Operators ending in ":" associates to the right 

> A :: B :: C => A :: (B :: C)

### Operations on Lists
All operations on lists can be expressed in terms of the following three:
- head: the first element of a list
- tail: a list excluding the first element
- isEmpty: 'true' if the list is empty, 'false' otherwise

	scala> fruits => "Apples"
	scala> diag3 => List(1,0,0)
	scala> empty => throw NoSuchException("head of empty list")

#### Exercise 4.3.1
Consider the pattern x :: y :: List(xs, ys) :: zs

What is the condition that describes most accurately the length L of the lists it matches?

	L >= 3, depending on if the last element zs is Nil

### Sorting Lists
Suppose we want to sort a list of numbers in ascending order:
- One way to sort the list List(7, 3, 9, 2) is to sort the tail List(3, 9, 2) to obtain List(2, 3, 9)
- The next step is to insert the head 7 in the right place to obtain the result List(2, 3, 7, 9)

The idea describes **Insertion Sort:**

	scala> def isort(xs: List[Int]): List[Int] = xs match
		case List() => List()
		case y :: ys => insert(y, isort(ys))

#### Exercise 4.3.2

	scala> def insert(x: Int, xs: List[Int]): List[xs] = xs match
		case List() => List(x)
		case y :: ys =>
			if x < y then x :: xs else y :: insert(x, ys) 

What's the worst-case complexity of insertion sort relative to the length of the input list N?

- proportional to $N * N$

This idea describes Insert Sort

#### Practice Quiz 4.3
1) Which statement about lists are true?

	- The 'head' method on a 'List ' always produces an element of type 'T'.

	- Lists are immutable

		Correct,  This statement is true: the elements of the list cannot be changed

	- All the elements of a list must have the same type

		Correct, This statement is true: the elements of a list must all have the same type

	- It is possible to decompose lists with pattern matching

		Correct, It is possible to use patterns such as 'p :: ps', 'Nil', 'List(a)'

## Lecture 4.4 - Enums
### Pure Data
In the previous sessions, you have learned how to model data with class hierarchies.
Classes are essentially bundles of functions operating on some common values represented as fields.

They are very useful abstraction, since they allow encapsulation of data.
But sometimes we just need to compose and decompose **pure data** without any associated functions.
Case classes and pattern matching work well for this task. 

### A Case Class Hierarchy
Here's our case class example again

	scala> trait Expr
	scala> object Expr:
		case class Var(s: String) extends Expr
		case class Number(n: Int) extends Expr
		case class Sum(e1: Int, e2: Int) extends Expr
		case class Prod(e1: Int, e2: Int) extends Expr
	
This time we have put all case classes in the Expr companion object, 
in order not to pollute the global namespace.

So it's `Expr.Number(1)` instead of `Number(1)`, for example. 
One can still "pull out" all the cases using an import

	scala> import Expr.*

Pure data definitions like these are called **algebraic data types**, or ADTs for short.

They are very common in functional programming. To make them even more convenient, 
Scala offers some special syntax.

### Enums for ADTs
an ** enum** enumerates all caes of an ADT and nothing else.

	scala> enum Expr:
		case Var(s: String)
		case Number(n: Int)
		case Sum(e1: Int, e2: Int)
		case Prod(e1: Int, e2: Int)

Same case class hierarchy as above, but avoids the `class` and `extends Expr` arguments 

### Pattern Matching on ADTs
Match expressions can be used on enums as usual.
For instance, to print expressions with proper parameterization

	scala> def show(e: Expr): String = e match
		case Expr.Var(x) => x
		case Expr.Number(n: Int) => n.toString
		case Expr.Sum(e1: Int, e2: Int) => e1.showP + " + " e2.showP
		case Expr.Prod(e1: Int, e2: Int) => e1.showP + " * " e2.showP

If you first `import Expr.*` then we can simply use the show method from before

	scala> def showP(e: Expr): String = e match
		case e: Sum => "(" + e.show + ")"
		case _ => e.show

### Simple Enums
Cases of an enum can also be simple values, without any parameters.

Define a Color type with values *Red*, *Green*, and *Blue*:

	scala> enum Color:
		case Red
		case Green
		case Blue

	scala> enum Color:
		case Red, Green, Blue

### Enum Pattern Matching
For pattern matching, simple cases count as constants:

	scala> enum DayOfWeek:
		case Monday, Tuesday, Wednesday, Thursday, Friday, Saturday, Sunday
	scala> import DayOfWeek.*
	scala> def isWeekend(day: DayOfWeek) day match
		case Saturday | Sunday => true
		case _ => false

As before, Enumerations can take parameters and can define methods:

	scala> enum Direction(val dx: Int, val dx: Int):
		case Right extends Direction(1, 0)
		case Up extends Direction(0, 1)
		case Left extends Direction(-1, 0)
		case Down extends Direction(0, -1)

		def leftTurn = Direction.values((ordinal + 1) % 4)
	scala> val r = Direction.Right
	scala> val u = x.leftTurn             // u = Up
	scala> val v = (u.dx, u.dy)           // v = (0, 1)

### More Fun With Enums
**Notes:**
- Enumeration cases that pass parameters have to use an explicit `extends` clause
- The expression `e.ordinal` gives the ordinal value of the enum case e. 
Cases start with zero and are numbered consecutively.
- `values` is an immutable array in the companion object of an enum that contains all enum values.
- Only simple cases have ordinal numbers and show up in values, parameterized xcases do not

### Enums are Shorthands for Classes and Objects
The direction enum is expanded by the Scala compiler to roughly the following structure:

	scala> abstract calss Direction(val dx: Int, val dy: Int): 
		def leftTurn = Direction.values((ordinal + 1) % 4)
	scala> object Direction:
		val Right = new Direction(1, 0) {}
		val Up = new Direction(0, 1) {}
		val Left = new Direction(-1, 0) {}
		val Down = new Direction(0, -1) {}

There are also complier-defined helper methods ordinal in the class and values and
valueOf in the companion object.

### Domain Modeling
ADTs and enums are particularly useful for domain modelling tasks where one needs to define a 
larger number of data types without attaching operations.

**Example:** Modeling Payments

	scala> enum Paymentmethod:
		def CreditCard(kind: Card, holder: String, number: Long, expires: Date)
		def payPal(email: String)
		def Cash

	scala> enum Card:
		case Visa, Mastercard, Amex

### Summary
In this unit, we covered two uses `enum` calsses, ADT
- as a shorthand for hierarchies of case classes, ADT
- as a way to define data types accepting alternative values

The 2 cases can be combined: an unum can comprise parameterized and simple cases at the same time.

Enums are typically used for pure data, where all operations on such data are defined elsewhere.

#### Practice Quiz 4.4

1) Algebraic Data Types (ADTs) in Scala

	- cannot be used with pattern matching

	- cannot be represented

	- can be represented with an enumeration 'enum'

		Correct, Enumerations allow us to represent ADTs with less boilerplate code 
			than traits. The compiler will translate the enum into a class hierarchy.

	- can be represented with a hierarchy of classes

		Correct, We can use traits and case classes to encode ADTs, as we saw with the 'Expr' type

## Lecture 4.5 - Subtyping and Generics
### Polymorphism
two principle form of polymorphism
- subtyping
- generics

In this session we'll look at their interactions. Main areas:
- boundaries
- variance

### Type Bounds
Consider the method `assertAllPos`
- Takes an `IntSet
- Return an `IntSet` if all elements are positive
- else throw exception

what woukld be the best type you can give the `assertAllPos` subroutine.
First thought is,

	scala> def assertAllPos(IntSet): IntSet = ...

Now one might express that `assertAllPos` takes
`Empty` sets to `Empty` sets and `NonEmpty` sets to `NonEmpty` sets.

	scala> def assertAllPos[S <: IntSet](r: S): S = ...

Here the `<: IntSet` is an **upper bound** of the type parameter S:

so S can only be instantiated with types that conform to IntSet
- `S <: T` means $S$ is a subtype of $T$
- `S >: T` means $S$ is a supertype of $T$, or $T$ is a subtype of $S$

#### Lower Bounds
You can use lower bounds for a type variable.

**Example** `[S >: NonEmpty]` introduces a type parameter S 
that can range only over supertypes of NonEmpty.

So S can be one of `NonEmpty`, `IntSet`, `AnyRef`, or `Any`. 
We'll see in the next session examples where lower bounds are useful.

#### Mixed Bounds
Possible to restrict $S$ to a range of values on the interval between `NonEmpty` and `IntSet`

**Example** `[S >: NonEmpty <: IntSet]` 

#### Covariance
There's another interaction between subtyping and type parameters we need to consider. Given:
`Non <: IntSet` is `List[NonEmpty] <: List[IntSet]` 

A list of non-empty sets is a special case of a list of arbitrary sets.

We call types for which this relationship holds **covariant** because their subtyping relationship varies
with the type parameter.

Does covariance make sense for all types, not just for `List`?

### Arrays
Arrays in Java are covariant, so one would have: `NonEmpty[] <: IntSet[]`

Reminder:
- An array of T elements is written T[] in Java
- In Scala we use *parameterized type syntax* Array[T]

#### Array Typing Problem
covariant subtyping causes a few problems, consider this (Java) example.

	Java> NonEmpty[] a = new NonEmpty[]{
		new NonEmpty(1, new Empty(), new Empty())};
	Java> IntSet[] b = a;
	Java> b[0] = new Empty()
	Java> NonEmpty s = a[0]

So we were able assign an `Empty` set to a variable of type `NonEmpty`.

![]("Desktop/Scala_Coursera/Unit_4/Pictures/NonEmpty_Empty_Assignment_Error.png")

Arrays are covariant in Java, but when using this property in Scala we can face runtime errors

#### The Liskov Substitution Principle
If `A <: B`, then everything one can do with a value of type $B$ one should also be able to do with a 
value of type $A$.

Let $q(x)$ be a property provable of objects x of type $B$.
Then $q(y)$ should be provable for objects $y$ of type $A$ where `A <: B`.

#### Exercise 4.5.1
The problematic array eample would be written as follows in Scala:

	scala> val a: Array[NonEmpty] = Array(NonEmpty(1, Empty(), Empty()))
	scala> val b: Array[IntSet] = a
	scala> b(0) = Empty()
	scala> val s: NonEmpty = a(0)

A type error in line 2, NonEmpty is a Subtype of IntSet; 
since types can not be covariant in Scala, opposed to Java.

#### Practice Quiz 4.5
1) What can we say about type 'T' in

	def f[T >: NonEmpty <: IntSet](t: T) = ???

	- 'T' cannot be 'NonEmpty'

	- 'T' cannot be 'IntSet'

	- 'T' is a subtype of 'IntSet' and a supertype of 'NonEmpty'

		Correct, 'T' is constrained to be any type in the interval from 'NonEmpty' and 'IntSet'

	- 'T' is a subtype of 'NonEmpty' and a supertype of 'IntSet'

2) The Liskov Substitution Principle defines

	- When a type can be considered a subtype of another type


	- How to reason about covariance with Java arrays.


	- When to use case classes instead of traits.

	Correct, The LSP states that 'A' is a subtype of 'B' if anything that one can do 
		with a value of type 'B' one can also do with a value of type 'A'

## Lecture 4.6 - Variance
As a rule, immutable types can be covariant, if some conditions on methods are met.

Roughly speaking, mutable data types should/can not be covariant.

### Definition of Variance
Say `C[T]` is a parameterized type and $A$, $B$ are types such that `A <: B`.

In general, there are three possible relationships between `C[A]` and `C[B]`:

	C[A] <: C[B]                                       C is covariant
	C[A] >: C[B]                                       C is contravariant
	neither C[A] nor C[B] is a subtype of the other    C is nonvariant
	
Scala lets you decalre the variance of a type by annotating the type parameter

	class C[+A] {...}                                  C is covariant  
	class C[-A] {...}                                  C is contravariant  
	class C[A] {...}                                   C is nonvariant  

#### Exercise 4.6.1
Assume the following type hierarchy and two fucntion types:

	scala> trait Fruit
	scala> trait Apple extends Fruit
	scala> trait Orange extends Fruit
	scala> type FtoO = Fruit => Orange
	scala> type AtoF = Apple => Fruit

According to the Liskov Substitution Principle which hierarchy holds?

FtoO <: AtoF, since Orange is a subclass of Fruit; it doesnt matter than Apple is not a superclass of Fruit

### Typing Rules for Functions
Generally, we have the following rule for subtyping between function types:

If `A2 <: A1` and `B1 <: B2`, then `A1 => B1 <: A2 => B2`. So functions are *contravariant* in their argument 
type(s) and covariant in theri result type.

This leads to the following revised definiton of the `Function1` trait:

	scala> trait Function1[-T, +U]:
		def apply(x: T): U

### Variance Checks
We have seen in the array example that the combination of covariance with certain operations is unsound.

In this case the problematic operation was the update operation on an array.

If we turn `Array` into a class, an `update` into a method, it would look like this:

	class Array[+T]:
		def update(x: T) = ...

The problematic combination is
- the covariant type parameter T
- which appears in parameter position of the method update

The Scala complier will check that there are no problematic combinations when compiling class 
with variance annotations.
- covariant type parameters can only appear in method results
- contravariant type parameters can only appear in method parameters
- invariant type parameters can appear anywhere

The precise rules are a bit more involved, fortunately the Scala compiler performs them for us.

#### Variance-Checking the Function Trait
Let's have a look again at `Function1`:

	scala> trait Function1[-T, +U]:
		def apply(x: T): U

Here,
- T is contravariant and appears only as a method parameter type
- U is covariant and appears only as a method result type

So the method is checks out

### Variance and Lists
Let's get back to the previous implementation of lists.

One shortcoming was that `Nil` had to be a class, whereas we would prefer it to be an object
(after all, there is only one empty list)

Since Lists are covariant, we can modify these results

	scala> trait List[+T]
		...
	scala> object Empty extends List[Nothing]
		...

`Nothing <: T`

### Idealized Lists
Here a definition of lists that implements all the cases we have seen so far

	scala> trait List[+T]:
		
		def isEmpty = this match
			case Nil => true
			case _ => false

		override def toString =
			def recur(prefix: String, xs: List[T]): String = xs match
				case x :: xs1 => prefix+x+recur(", ", xs1)
				case Nil => ")"
			recur("List(", this))
	scala> case class ::[+T](head: T, tail: List[T]) extends List[T]
	scala> case object Nil extends List[Nothing]
	scala> extension [T](x: T) def :: (xs: List[T]): List[T] = ::(x, xs)
	scala> object List:
		def apply() = Nil
		def apply[T](x: T) = x:: Nil
		def apply[T](x1: T, x2: T) = x1 :: x2 ::Nil

Later we'll learn how to do this using just a single apply method using *vararg* parameter.

#### Making Classes Covariant
Sometimes, we have to put in a bit of work to make a class covariant.

Consider adding a `prepend` method to `List` which prepends a given element yielding a new list.
Intuitions leads to the solution, 

	scala> trait List[+T]:
		def prepend(elem: T): List[T] = ::(elem, this)

but this will not work. Why does this code not type-check?
- prepend's right-hand side contains a type error

at the root level of recursion Nil will not be a subtype of T

#### Prepend Violates LSP
Indeed, the compiler is right to throw out `List` with prepend because 
it violates the Liskov Substitution Principle.

Here's something one can do with a list of type `List[Fruit]`:
> xs.prepend(Orange)

But the same operation on a list ys of type `List[Apple]` would lead to a type error
> ys.prepend(Orange)

would lead to 
	
	^type mismatch
	required: Apple
	found: Orange

So, List[Apple] cannot be a subtype of List[Fruit]

### Lower Bounds
But prepend is a natural method to have on immutable lists!

How can we amke its variance 'correct'?

We can use a *lower bound*:

	def prepend [U >: T](elem: U): List[U] = ::(elem, this)

this passes variance checks, because
- *covariant* type parameters may appear in *lower bounds* of method type parameters
- *contravaiant* type parameters may appear in *upper bounds*

#### Exercise 4.6.2
Assume prepend in trait List is implemented lkike this:
	
	def prepend [U >: T](elem: U): List[U] = ::(elem, this)

What is the result type of this function:

	def f(xs: List[Apple], x: Orange) = xs.prepend(x)

`List[Fruit]`, in a list of Apples, get the element of type Orange and finds the 
smallest supertype of both elements.

### Extension Methods
We can obtain the same functionality through *extension methods*

The need for lower bounds was essential to decouple the new parameter of the class and the parameters of
the newly created object. Using an extension methods such as :: above, we can sidestep this problem.

	scala> extension [T](x: T) def :: (xs: List[T]): List[T] = ::(x, xs)
	
#### Practice Quiz 4.6
1) Which class is covariant?

	scala> class B[-T]

	scala> class A[+T]

		Correct, 'A' is covariant. Therefore, if 'U <: V' then

			A[U] <: A[V]

	scala> class C[T]

2) Functions are

	- covariant in their argument type and nonvariant in their result type

	- contravariant in their argument type and covariant in their result type

	- nonvariant in their argument type and contravariant in their result type

	- covariant in their argument type and contravariant in their result type

		Correct, The signature of 'Function1' is

			trait Function1[-U, +R]:
				def apply(x: U): R
