# Unit 2: Higher Order Functions
## Lecture 2.1 - Introduction to Higher Order Functions

**Definition: Higher Order Functions** take other functions as parameters or return functions as results

Functional Language first treats functions as first class values.
This means functions can be passed as parameters and returned as results.
Provides a flexible way to compose programs

### Higher Order Implementations

Take the sum of integers between a and b:

	scala> def sumInts(a: Int, b: Int): Int = 
		if a > b then 0 else a + sumInts(a + 1, b)

And the sum of cubes of all intgers between a and b

	scala> def cube(x: Int): Int = x * x * x
	scala> def sumCubes(a: Int, b: Int): Int = 
		if a > b then 0 else cube(a) + sumCubes(a + 1, b)

And the sum of the factorials of all intgers between a and b

	scala> def fact(n: Int): Int = if n==0 then 1 else n*fact(n-1)
	scala> def sumFacts(a: Int, b: Int): Int = 
		if a > b then 0 else fact(a) + sumFacts(a + 1, b)

These are all special cases of the sum function $\sum_{n=a}^bf(n)$. 
To abstract these special cases of sum, create higher order function sumFuncs.

	scala> def sumFuncs(f: Int => Int, a: Int, b: Int): Int =
		if a > b then 0 else f(a) + sumFuncs(f, a + 1, b)

Apply to each sums' instance 

	scala> def id(x: Int): Int = x
	scala> def sumInts(a: Int, b: Int): Int = sumFuncs(id, a, b)
	scala> def sumCubes(a: Int, b: Int): Int = sumFuncs(cube, a, b)
	scala> def sumFacts(a: Int, b: Int): Int = sumFuncs(fact, a, b)

$A => B$, takes an argument of type $A$ and returns a result of type $B$.
So $Int => Int$ is a funciton that maps integers to integers.

### Anonymous Function

Passing functions as parameters leads to the creation of many small function.
Naming and tracking all such arguments can be tedious, hence the rise of Anonymous Functions

For example do not need to first def string to use it,

	scala> def str = "abc"; println(str)

Because strings exist as literals, can just write
	
	scala> println("abc")

**Definition Anonymous Functions** are function literals, allowing us write a function without a name

Note, return type parameter is not needed if it can be inferred by the compiler in context- 
except for specific instances such as loop.

Create an Anonymous function that raises its arguments to a cube. 
	
	scala> (x: Int) => x * x * x

Create an Anonymous function that sums two arguments. 
	
	scala> (x: Int, y: Int) => x + y

Anonymous fucntions are known as syntactic sugar since they can always be expressed by definitions.
> $(x_1: T_1, ..., x_n: T_n) => E$, and def $f(x_1: T_1, ..., x_n: T_n) = E$
	
Apply to each sums' instance with anonymous fucntions

	scala> def sumInts(a: Int, b: Int): Int = sumFuncs(x => x, a, b)
	scala> def sumCubes(a: Int, b: Int): Int = sumFuncs(x => x*x*x, a, b)

Factorial has non-trivial logic and is best left as a definition

#### Exercise 2.1

The higher order sum function uses linear recursion, write a tail-recursive version.

	scala> def sumFuncs(f: Int => Int, a: Int, b: Int): Int =
		@tailrec
		def loop(a: Int, acc: Int): Int = 
			if a > b then acc 
			else loop(a + 1, acc + f(a))
		loop(a, 0)

#### Practice Quiz 2.1

	1) Higher-order functions
		
		- can return functions

			Correct, def chain(x: Int): Int => Int = (y: Int) => x + y
				is a valid higher-order function returning a function

		- can take functions as parameters

			Correct, def applicator(g: Int => Int, x: Int): Int = g(x)
				is a valid higher-order function taking a function parameter

		- are introduced by the "higher" keyword

		- cannot be recursive

## Lecture 2.2 - Currying

Motivation, There is still of repitiion within the sumFuncs arguments
	
	scala> def sumInts(a: Int, b: Int): Int = sumFuncs(x => x, a, b)
	scala> def sumCubes(a: Int, b: Int): Int = sumFuncs(x => x*x*x, a, b)
	scala> def sumFacts(a: Int, b: Int): Int = sumFuncs(fact, a, b)

How can we eliminate the repitition of the a and b terms?
We can create a new sumFuncs that takes in a function, maping integers to integers, 
and returns a function that maps a tuple of integers to an integer

	scala> def sumFuncs(f: Int => Int): (Int, Int) => Int = 
		def sum(a: Int, b: Int): Int = 
			if a > b then 0
			else f(a) + sum(a + 1, b)
		sum
	scala> def sumInts = sumFuncs(x => x)
	scala> def sumCubes = sumFuncs(x => x*x*x)
	scala> def sumFacts= sumFuncs(fact)

Now to call the functions simply apply to a tuple of Integers

	scala> sumInts(1,10) + sumFacts(10, 20)

### Conscutive Stepwise Applications 

In the previous example, can we avoid the sumInts, sumCubes, ... middlemen?

	scala> sumFuncs(fact)(1,10)

The sumFuncs is first applied to a function that maps integers to integers, then is applied to a tuple and returns an integer.
May look strange, but is similar to array selection with multiple dimensions $a(i)(j)$

### Multiple Parameter Lists

The definition of higher order functions are so useful in Scala they have a specific syntax. 
This is an exquivalent definition of the sumFuncs, without the nested sum fucntion

	scala> def sumFuncs(f: Int => Int)(a: Int, b: Int): Int = 
		if a > b then 0 else f(a) + sumFuncs(f)(a + 1, b)

#### Multiple Parameter List Abstraction

In general, a definition of a fucntion with multiple parameter lists 
> def $f(ps_1)...(ps_n) = E$ 

where $n > 1$, is equivalent to 
> def $f(ps_1)...(ps_{n-1})$ = {def $g(ps_n) = E;g$}

where g is a fresh identifier. Or for short:
> def $f(ps_1)...(ps_{n-1}) = (ps_n => E)$

Is also equivalent to the currying style,
> def $f = (ps_1 => (ps_2 => ...(ps_n => E)...))

can write every function as a sequence of anonymous functions

	scala> sumFuncs((Int => Int) => ((Int, Int) => Int)) = ...

Note functional types associate to the right so,

	scala> sumFuncs((Int => Int) => (Int, Int) => Int) = ...

is possibly less clear, but is noetheless equivalent

#### Exercise 2.2

1) write a product function that calculates the product of the values of a function for the points on a given interval

	scala> def prod(f: Int => Int)(a: Int, b: Int): Int = 
		@tailrec
		def loop(n: Int, acc: Int): Int =
			if n > b then acc 
			else loop(n + 1, f(n) * acc)
		loop(a, 1)

2) Write factorial in terms of product

	scala> def factorial(n: Int) = prod(x => x)(1, n)
	scala> factorial(5)

3) Can you write more general function, which generalizes both sum and product

	scala> def mapReduce(f: Int => Int, combine: (Int, Int) => Int, base: Int)(a: Int, b: Int): Int = 
		@tailrec
		def loop(n: Int, acc: Int): Int =
			if n > b then acc 
			else loop(n + 1, combine(f(n), acc))
		loop(a, base)
 	scala> def sum(f: Int => Int) = mapReduce(f, (x, y) => x+y, 0)
 	scala> def prod(f: Int => Int) = mapReduce(f, (x, y) => x*y, 1)
	scala> sum(factorial)(1,5)
	scala> prod(factorial)(1,5)

#### Practice Quiz 2.2

	1) Which statements are true about the snippet below?

		def f(a: String)(b: Int)(c: Boolean): String =
			"(" + a + ", " + b + ", " + c + ")"

		val partialApplication1 = f("Scala")
		val partialApplication2 = partialApplication1(42)

		- The code compiles and the type of 'partialApplication2' is 'Boolean => String'

			Correct, Multiple parameters list allow this form of partial application so the code compiles. 
				'partialApplication2' is the result of the application of 'f' with an argument of type 
				String and an argument of type Int. The only parameter list to evaluate is the '(c: Boolean)'

		- The code compiles and the type of 'partialApplication1' is 'Int => Boolean => Int'

		- The snippet does not compile

	2) Consider a function 'f: Int => String => Double'. What is the correct way to use it?

		- f(3, "Scala")

		- f(3)("Scala")

			Correct, Function types associate to the right, the type can be rewritten as 'Int => (String => Double)'

		- f((x: Int) => "The integer " + x)

## Lecture 2.3 - Example: Finding Fixed Points

**Definition:** A number $x$ is a **fixed point** of function $f$ if
> $f(x) = x$
	
For some function f, we can locate a fixed point by starting with an initial estimate, 
then applying f in a repetitive way until the change is "sufficiently small"
> $x, f(x), f(f(x)), ...$

Leads to the following function for finding a fixed point

	scala> val tolerance = 0.0001
	scala> def isCloseEnough(x: Double, y: Double) =
		abs((x - y) / x) < tolerance
	scala> def fixedPoint(f: Double => Double)(firstGuess: Double): Double = 
		def iterate(guess: Double): Double = 
			val next = f(guess)
			if isCloseEnough(guess, next) then next
			else iterate(next)
		iterate(firstGuess)

### Return to Square Root

A square root is defined as such
> $sqrt(x) = y$, such that $y*y = x$

Or
> $sqrt(x) = y$, such that $y = x / y$

And $sqrt(x)$ is a fixed point of the function $(y => x/y)$, 
or $sqrt(x) => x/ sqrt(x)$. 
So calculate sqrt by iterating towards a fixed point

	scala> def scala(x: Double) = 
		fixedPoint(y => x/y)(1.0)

But this is application is divergent. If add println statement to iterate subroutine of fixed point, 
we'll see 
> $sqrt(2)$ oscillates between 1 and 2

### Average Damping Solution

To prevent this we can apply a dampener to these oscillations, forcing the estimation to not vary "too much,"
and the series to converge> This is achieved by averaging successive values of original sequence

	scala> def sqrt(x: Double) = fixedPoint( y => (y + x / y) / 2)(1.0)

> now  $sqrt(2)$ results in 1.5, 1.4166666666666666, 1.4142156862745097, 1.4142156862746899, 1.4142156862746899

The following example shows how useful higher Order functions can be.
Again consider iteration towards a fixed point. 
Observe that $\sqrt{x}$ is a fixed point of the function $y => x / y$

	scala> def averageDamp(f: Double => Double)(guess: Double): Double = (guess + f(guess)) / 2

#### Exercise 2.3

Rewrite a square root function using fixedPoint and averageDamp

	scala> def sqrt(x: Double) = fixedPoint(averageDamp(y => x/y))(1.0)

## Lecture 2.4 - Scala Syntax Summary

Language elements seen so far. Below we give their context-free syntax in Extended Backus-Naur form (EBNF), where
	
	| denotes an alternative,
	[...] an option (0 or 1),
	(...) a repition (0 or more)

### Types

- Type = SimpleType | FunctionType
- FunctionType = SimpleType => Type | (Types) => Type
- SimpleType = Ident
- Types = (Type, Type, ...)

A **Type** can be
- Numeric: Int, Double, Byte, Short, Long, Float, Char
- Boolean: true or false
- String
- Function Type: like Int => Int, (Int, Int) => Int

### Expressions

- Expr = InfixExpr | FunctionExpr | if Expr then Expr else Expr
- InfixExpr = PrefixExpr | InfixExpr Operator InfixExpr: $x + 1$
- Operator = ident
- PrefixExpr = [+, -, !, ~] SimpleExpr: $-x$
- SimpleExpr = ident | literal | SimpleExpr . ident | Block: $x | 5 | "abc" | x.y$
- FunctionExpr = Bindings => Expr: $x => x*x$
- Bindings = ident | (Binding, Binding, ...): $(x, y) => x + y$
- Block = {Def; Expr} | <indent> {Def;} Expr <outdent>: {val x = 2; x*x}

Used Examples
- An **Identifier** such as x, is Good Enough
- A **Literal**, like 0, 1.0, "abc"
- A **Function Application**, like $sqrt(x)$
- An **Operator Application** like $-x$, $y + x$
- A **Selection**, like $math.abs$
- A **Conditional Expression**, like if $x < 0$ then $-x$, else $x$
- A **Block**, like {val $x = math.abs(y); x*2$}
- An **Anonymous Fucntion**, like $x => x+1$

### Definitions

A **Definition** can be
- A **Function Definition**, like def $square(x: int) = x*x$
- A **Value Definition**, like val $y = square(2)$

Definition Abstractions
- Def = FunDef | ValDef
- FunDef = def ident (Parameters, ...)...: Type = Expr
- ValDef = val ident: Type = Expr

A **Parameter** can be:
- A **Call-by-value**, like $(x: Int)$,
- A **Call-by-name**, like $(y: => Double)$,

Parameter Abstractions
- Parameter = ident [:, =>] Type
- Parameters = (Parameter, Parameter, ...)

## Lecture 2.5 - Functions and Data

Learn how functions encapsulate data structures.
For example, consider Rational Number. How can we design a package for doing rational arithmetic

**Definition:** a **rational number** $\frac{x}{y}$, is represented by 2 integers
- its numerator $x$
- its denominator $y$

### Classes

Consider a class definition of Rational numbers, where

	scala> class Rational(x: Int, y: Int):
		def numer = x
		def denom = y

This definition introduces 2 new entities
- A **type**, named Rational
- A **constructor** Rational to create elements of this type

**Note** Scala keeps the names of types and values in different namespaces, 
thus the two entities named Rational will not conflict

**Definition:** elements of a class type are called **objects**

To create an object by calling the constructor of a class

	scala> val x = Rational(1,2)
	val x: Rational = Rational@4f1fb828
	scala> x.numer
	scala> x.denom

### Rational Arithmetic

> $\frac{n_1}{d_1}+\frac{n_2}{d_2}$ = $\frac{n_1d_2 + n_2d_1}{d_1d_2}$

	scala> def addRational(first: Rational, second: Rational): Rational = 
		Rational(first.numer*second.denom + second.numer*first.denom,
			first.denom*second.denom)

> $\frac{n_1}{d_1}-\frac{n_2}{d_2}$ = $\frac{n_1d_2 - n_2d_1}{d_1d_2}$

	scala> def subRational(first: Rational, second: Rational): Rational = 
		Rational(first.numer*second.denom - second.numer*first.denom,
			first.denom*second.denom)

> $\frac{n_1}{d_1}*\frac{n_2}{d_2}$ = $\frac{n_1n_2}{d_1d_2}$

	scala> def multRational(first: Rational, second: Rational): Rational = 
		Rational(first.numer*second.numer, first.denom*second.denom)

> $\frac{n_1}{d_1}/\frac{n_2}{d_2}$ = $\frac{n_1d_2}{d_1n_2}$

	scala> def divRational(first: Rational, second: Rational): Rational = 
		Rational(first.numer*second.denom, first.denom*second.numer)

> $\fac{n_1}{d_1} = \frac{n_2}{d_2}$ iff $n_1d_2 = d_1n_2$

	scala> def mkString(rational: Rational): String =
		s"${rational.numer}/${rational.denom}"

### Methods

**Definition:** One can further package functions operating on data 
abstraction in the data abstraction itself- **methods**

	scala> class Rational(x: Int, y: Int):
		def numer = x
		def denom = y

		def neg = Rational(-numer, denom)
		def add(rational: Rational): Rational = 
			Rational(numer*rational.denom + rational.numer*denom,
				denom*rational.denom)
		def sub(rational: Rational): Rational = 
			add(rational.neg)
		def mult(rational: Rational): Rational = 
				Rational(numer*rational.numer, denom*rational.denom)
		def div(rational: Rational): Rational = 
				Rational(numer*rational.denom, denom*rational.numer)
		override def toString = s"${numer}/${denom}"

**Note** the modifer override declares that toString redefines 
a method that already exists (in the class java.lang.Object)

	scala> val x = Rational(1,2)
	scala> val y = Rational(4,7)
	scala> val z = Rational(3,2)
	scala> x.add(y).mult(z)

#### Practice Quiz 2.5

	1) The 'class' keyword introduces

		- A new type

		- a constructor

		- All of the above

			Correct, 'class' introduces two entities: a new type and a constructor to create elements of that type

	2) Definitions within a class body and introduced with the keyword 'def' are called

		- constructors

		- methods

			Correct, Methods are functions operating on class data defined inside the class

		- inner functions

		- anonymous functions

## Lecture 2.6 - More Fun With Rationals

This constuctor currently does not have a reduction method availible;
Reduce to smallest numerator and denominator by dividing both with a divisor.

	scala> class Rational(x: Int, y: Int):
		private def gcd(a: Int, b: Int): Int =
			if b == 0 then a else gcd(b, a%b)
		private val bigFact = gcd(x, y)
		def numer = x / bigFact
		def denom = y / bigFact
		...

### Self Reference
	
On the inside of a class, the name **this** respresents the object on which the current method is executed.
Consider the methods less and max
> this: $x_1y_2 < x_2y_1$

> max: returns x_1y_2$ or $x_2y_1$ dependent on which is greater

	scala> class Rational(x: Int, y: Int):
		...
		def less(that: Rational): Boolean =
			numer * that.denom < that.numer * denom
		def max(that: Rational): Rational = 
			if this.less(that) then that else this
		...

### Preconditions

Let's say our Rational class requires that the denominator is positive.
We can enforce this by calling the reuire function

	scala> class Rational(x: Int, y: Int):
		require(y > 0, "denominator must be positive")
		...

**require** is a predefined function
- 1st arg: condition
- 2nd arg: optional message string

If the condition is false, an Illegal ArgumentExpection is thrown with the given message string: and program is terminated.

**Assertions** takes a  condition and an optional message string as parameters.
	
	scala> ...	
		val x = sqrt(x)
		assert(x>= 0)

failing an assertion will throw an AssertionError.

The difference is reflected in their intent
- require is uesd to enforce a precondition on the caller of a fucntion
- assert is used as to check the code of the function itself

### Constructors

In Scala, a class implicitly introduces a constructor. 
This one is called the **primary constructor** of the class.
- takes the parameters of the class
- executes all statements in the class body (such as the require a couple of slides black)

Scala allows for the definition of **auxiliary constructors**

	scala> class Rational(x: Int, y: Int):
		def this(x: Int) = this(x, 1)
		...
	scala> val x = Rational(2)
		val x: Rational = 2/1

Full Rational Class

	class Rational(x: Int, y: Int):
		// Class Requirements
		require(y > 0, s"denominator must be positive, was $x/$y")
		def this(x: Int) = this(x, 1) // Managing Single Argument Rationals

		// Reduce Rationals
		private def gcd(a: Int, b: Int): Int =
			if b == 0 then a else gcd(b, a%b)
		private val bigFact = gcd(x.abs, y)
		def numer = x / bigFact
		def denom = y / bigFact

		// Combination of Rationals
		def neg = Rational(-numer, denom)
		def add(rational: Rational): Rational = 
			Rational(numer*rational.denom + rational.numer*denom,
				denom*rational.denom)
		def sub(rational: Rational): Rational = 
			add(rational.neg)
		def mult(rational: Rational): Rational = 
				Rational(numer*rational.numer, denom*rational.denom)
		def div(rational: Rational): Rational = 
				Rational(numer*rational.denom, denom*rational.numer)

		// Comparison of Rationals
		def less(that: Rational): Boolean =
			numer * that.denom < that.numer * denom
		def max(that: Rational): Rational = 
			if this.less(that) then that else this

		override def toString = s"${numer}/${denom}"
	end Rational

#### End Markers

End Markers are also allowed for other constructs

	def sqrt(x: Double): Double =
		...
	end sqrt
	
	if x >= 0 then
		...
	else
		...
	end if

**Note** If the end marker terminates a control expresison such as if, the beginning keyword is repeated.

#### Exercise 2.6

Modify gcd such that rational numbers are kept unsimplified internally, until numbers are converted to strings

	class Rational(x: Int, y: Int):
		// Class Requirements and Definitions
		require(y > 0, s"denominator must be positive, was $x/$y")
		def this(x: Int) = this(x, 1) // Managing Single Argument Rationals
		def numer = x
		def denom = y

		// Combination of Rationals
		def neg = Rational(-numer, denom)
		def add(rational: Rational): Rational = 
			Rational(numer*rational.denom + rational.numer*denom,
				denom*rational.denom)
		def sub(rational: Rational): Rational = 
			add(rational.neg)
		def mult(rational: Rational): Rational = 
				Rational(numer*rational.numer, denom*rational.denom)
		def div(rational: Rational): Rational = 
				Rational(numer*rational.denom, denom*rational.numer)

		// Comparison of Rationals
		def less(that: Rational): Boolean =
			numer * that.denom < that.numer * denom
		def max(that: Rational): Rational = 
			if this.less(that) then that else this

		// Reduce Rationals
		private def gcd(a: Int, b: Int): Int =
			if b == 0 then a else gcd(b, a%b)
		private val bigFact = gcd(x.abs, y)

		override def toString = s"${numer/bigFact}/${denom/bigFact}"
	end Rational

Do clients observe the same behavior when interacting with the rational class
> yes for small sizes of denominators and numerators and small numbers of operations.
Potential for a stack overflow since we could approach the upper limit of Int before the reduction step

#### Practice Quiz 2.6

	1) private members of a class

		- Can only be accessed from the class constructor

		- Can only be accessed from class methods

		- Can only be accessed from inside the class

		- Can only be accessed in the file where the class is defined

			Correct, Private members can be accessed from methods and values defined inside the same class

## 2.7 - Evaluations and Operators
### Classes and Substitutions

We previously defined the meaning of a function application using a computation model based on substitution.
New we extend this model to classes and objects.

**Question:** How is an instantiation of the class $C(e_1, ..., e_m)$ evaluated?

**Answer:** the expression arguments e_1, ..., e_m are evaluated like the arguments of a normal function.
That's it. The resulting expression, $C(e_1, ..., e_m)$, is already a value

Suppose class definition,class $C(x_1, ..., x_m)${$... $def$ f(y_1, ..., y_n) = b ...$}.
Where
- The formal parameters of the class are x_1, ..., x_m
- The class defines a method of $f$ with formal parameters $y_1, ..., y_n$

**Question:** How is the following expression evaluated? $C(v_1, ..., v_m)f(w_1, ..., w_n)$

**Answer:** the expression $C(v_1, ..., v_m)f(w_1, ..., w_n)$ is rewritten to:
- the substitutions of the formal parameter $y_1, ..., y_n$ of the function $f$ by the argumentts $w_1, ..., w_n$
- the substitutions of the formal parameter $x_1, ..., x_n$ of the function $f$ by the argumentts $v_1, ..., v_n$
- the substitutions of the self reference this by the value of the object C(v_1, ..., v_m)

> [$w_1/y_1, ..., w_n/y_n$] [$v_1/x_1, ..., v_m/x_m$] [$C(v_1, ..., v_m)$/this] $b$

### Object Rewriting Example

	scala> Rational(1, 2).numer // 1

[$\frac{1}{x}, \frac{2}{y}$] [ ] [Rational(1,2)/this] => x

	scala> Rational(1, 2).less(Rational(2,3)) // true

[$\frac{1}{x}, \frac{2}{y}$] [Rational(2,3)/that] [Rational(1,2)/this] =>
this.numer * that.denom < that.numer * this.denom = 

Rational(1,2).numer * Rational(2,3).denom < Rational(1,2).denom * Rational(2,3).numer =>
$1 * 3 < 2 * 2$ => true

### Extension Methods

Having to define methods that belong to a class inside the class itself can lead to very large classes, and is not very modular.

Methods that do not need to access the internals of a class can be defined as extension methods.

For instance

	scala> extension (r: Rational)
		def min(s: Rational): Boolean = if s.less(r) then s else r
		def abs: Rational = Rational(r.numer.abs, r.denom)

Extensions of a class are visible if they are listed in the companion object iof a class, 
or if they are defined or imorted in the current scope.
Members of visible extensions of class C can be called as if they were members of C E.G.

	scala> Rational(1, 2).min(Rational(2, 3))

**Caveats:**
- Extensions can only add new members, not override existing ones
- Extensions cannot refer to other class members via this

### Substitutions

Extension method substitution works like normal subsstitution, but
- instead of this it's the extension parameter that gets substituted
- class parameters are not vivible, so do not need to be substituted at all

	scala> Rational(1, 2).min(Rational(2, 3))

[Rational(1,2)/r] [Rational(2,3)/s] if x.less(r) then s else r = 

if Rational(2,3).less(Rational(1,2)) then Rational(2,3) else Rational(1,2)

### Operators

In principle, the rational number defined by Rational are as naturtal as intergers.
but for the user of these abstractions, there is a noticeable difference.
- We write x + y, if x and y are integers, but
- We write r.add(s) if r and s are rational numbers

Scala can eliminate these differences

**Step 1** Relaxed Identifiers

Operators such as + or < count as identifiers in Scala
Thus an identifier can be
- **Alphanumeric:** Starting with a letter, folllwed by a sequence of letters ort numbers
- **Symbolic:** staring with an operator symbol, followed by other operator symbols
- The underscore character '_' counts as a letter
- Alphanumeric identifiers can also end in an underscore, followef by some operator symbols

Examples of identifiers: [x1, *, +?%&, vector_++, counter_=]

Since operators are identifiers, its possible to use them as method names

	scala> extension (x: Rational)
		def + (y: Rational): Rational = x.add(y)
		def * (y: Rational): Rational = x.mult(y)
		...

This allows rational number to be used like Int and Double:

	scala> val x = Rational(1, 2)
	scala> val y = Rational(1, 3)
		x*x + y*y

**Step 2** Infix Notation

An operator method with a single parameter can be used as an infix operator

An alphanumeric method with a single parameter can also be used as an infix operator if its declared with an 
infix modifier. E.G

	scala> extension(x: Rational)
		infix def min(that: Rational): Rational = ...

It is therefore possible to write

	r + s				r.+(s)
	r < s				r.<(s)
	r min s				r.min(s)

### Precedence Rules

The **Precedence** of an operator is determined by its first character. 

The following table list the characters in increasing order of priority precedence:

	(all letters)			1 + 2 * 3
	|
	^
	&
	< >
	= !
	:
	+ -
	* / %
	(all other special characters)

#### Exercise 2.7

Provide a fully parenthesized version of

	a + b ^? c ?^ d less a ==> b | c

Even binary operation needs to be put into parentheses,
but the structure of the expression should not change.

	((a + b).^?(c ?^ d)).less((a ==> b )| c)

#### Practice Quiz 2.7

	1)Extension methods

		- can use 'this'

		- can override existing members

		- can define new members for a type

			Correct, Extension methods allow you to define methods for a class outside the 
			body of the class, improving modularity and leading to shorter class definitions

		- can define infix operators

			Correct, the 'infix' keyword can be used for this purpose with methods defined in extension blocks

		- can access private members of the class that they extend

## Assignment

In this assignment, you will work with a functional representation of sets based on the mathematical notion of 
characteristic functions. The goal is to gain practice with higher-order functions.

Write your solutions by completing the stubs in the FunSets.scala file.

### Representation

We will work with sets of integers.

As an example to motivate our representation, how would you represent the set of all negative integers? 
You cannot list them all... one way would be to say: if you give me an integer, I can tell you whether 
it's in the set or not: for 3, I say 'no'; for -1, I say yes.

Mathematically, we call the function which takes an integer as argument and which returns a boolean indicating whether the 
given integer belongs to a set, the characteristic function of the set. For example, we can characterize the set of negative 
integers by the characteristic function $(x: Int) => x < 0$.

Therefore, we choose to represent a set by its characteristic function and define a type alias for this representation:

	scala> type FunSet = Int => Boolean

	scala> trait FunSetsInterface: 
		type FunSet = Int => Boolean
		def contains(s: FunSet, elem: Int): Boolean
		def singletonSet(elem: Int): FunSet
		def union(s: FunSet, t: FunSet): FunSet
		def intersect(s: FunSet, t: FunSet): FunSet
		def diff(s: FunSet, t: FunSet): FunSet
		def filter(s: FunSet, p: FunSet): FunSet
		def forall(s: FunSet, p: FunSet): Boolean
		def exists(s: FunSet, p: FunSet): Boolean
		def map(s: FunSet, f: Int => Int): FunSet
		def toString(s: FunSet): String
	scala> trait FunSets extends FunSetsInterface:
		def contains(s: FunSet, elem: Int): Boolean = s(elem)
		def singletonSet(elem: Int): FunSet = x => x == elem
		def union(s: FunSet, t: FunSet): FunSet = x => contains(s, x) || contains(t, x)
		def intersect(s: FunSet, t: FunSet): FunSet = x => contains(s, x) && contains(t, x)
		def diff(s: FunSet, t: FunSet): FunSet = x => contains(s, x) && !contains(t, x)
		def filter(s: FunSet, p: FunSet): FunSet = x => p(x) && contains(s, x)
		val bound = 1000
		def forall(s: FunSet, p: FunSet): Boolean =
			def iter(a: Int): Boolean =
				if a <= -bound then true
				else if diff(s, p)(a) then false
				else iter(a - 1)
			iter(bound)
		def exists(s: FunSet, p: FunSet): Boolean = !forall(s, x => !p(x))
		def map(s: FunSet, f: Int => Int): FunSet = x => exists(s, y => (x == f(y)))
		def toString(s: FunSet): String =
			val xs = for i <- (-bound to bound) if contains(s, i) yield i
			xs.mkString("{", ",", "}")
		def printSet(s: FunSet): Unit =
			println(toString(s))
	scala> object FunSets extends FunSets		



