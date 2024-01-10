# Functions and Evaluation
## Lecture 1.1 - Programming Paradigms

The primary objective is to teach you functional programming from first principles, 
you're going to see functional programs, methods to construct them, and ways to reason about them

#### Practice Quiz 1.1

	1) Select the statements that are true about functional programming
	
		- helps with the complexity of concurrency and parallelism
		
			Correct, Immutable data can be safely shared by multiple threads, 
			and higher-order functions are convenient to model asynchronous control-flow
		
		- focuses on functions and immutable data
		
			Correct, Immutable data, functions and higher order functions are the key concepts in FP
		
		- is possible only in functional programming languages
		
		- relies on mutable variables, loops and assignments.

## Lecture 1.2 - Elements of Programming

Every non-trival Programming Language provides:
	
	- primitive expressions represent the simplest elements
	- way to combine expression
	- ways to abstract expressions, introduce name for which expression can be referred to

### Read-Eval-Print-Loop
	
REPL is an interactive shell that allows for immediate feedback from inputs.
To start the REPL type scala in terminal (must have Java)

### Evaluation

A "Name" is evaluated by replacing it with the right hand side of its definition. 
In a call-by-value expression the process terminates once fully evaluated; 
In call-by-name the expression only checks data types conformity and lazly compute value as needed. 
	
	1) Take left-most operator
	2) Evaluate its operands (left before right)
	3) Apply the operator to the operands

	EX1: 
	scala> def pi = 3.1415
	scala> def radius = 10
	scala> val circum = (2*pi)*radius
		(2*pi)*radius
		(2*3.1415)*radius
		(6.28318)*10
		vaL circum: Double = 62.8318

	EX (call-by-value):
	scala> def square(x:Double): Double = x*x
	scala> def sumOfSquare(x:Double, y:Double): Double = x+x
	scala> val sum = sumOfSquares(3, 2+2)
		sumOfSquares(3, 4)
		square(3) + square(4)
		3*3 + square(4)
		9 + square(4)
		9 + 4*4
		9 + 16
		val sum: Double = 25

	EX (call-by-name):
	scala> def sumOfSquare(x: => Double, y: => Double): Double = x+x
	scala> val sum = sumOfSquares(3, 2+2)
		square(3) + square(2+2)
		3*3 + square(2+2)
		9 + square(2+2)
		9 + (2+2)*(2+2)
		9 + (4)*(2+2)
		9 + (4)*(4)
		9 + 16
		val sum: Double = 25


This subsitution model derives from lambda calculus- gave rise to programming

#### Primative Data Types

	Int 32-bit integers
	Long 64-bit integers
	Float 32-bit floating point numbers
	Double 64-bit floating point numbers
	Char 16-but unicode characters
	Short 16-bit integers
	Byte 8-bit integers
	Boolean boolean values are true and false

#### Termination

Does every expression reduce in a finite amount of steps?	

	scala> def loop: Int = loop
		Infinite recursive call

#### Call-by-name vs Call-by-value

Call-by-value: the function argument is evaluated only once

Call-by-name: the function argument is not evaluated if the corresponding parameter is unused in the evaluation of the function body

#### Exercise 1.2.1

	scala> def test(x: => Int, y: Int) = x*x
	scala> test(7, 8)
		same amount of time with either strategy

	scala> test(3+4, 8)
		call-by-value, want to reduce 3+4 first otherwise it will be evaluated twice

	scala> test(7, 2*4)
		call-by-name, want to avoid evaluating 2*4 because its unsed in function body

	scala> test(3+4, 2*4)
		call-by-value for the x position and call-by-name for y for reasons above

#### Practice Quiz 1.2

	1) in the call-by-name evaluation strategy
		
		- every expression reduces to a value in a finite number of steps
		
		- functions are applied to unreduced arguments
	
			Correct, The interpreter does not reduce the arguments before rewriting the function application
	
		- functions are applied to reduced arguments

## Lecture 1.3 - Evaluation Strategies and Termination

If CBV evaluation of an expression e terminates, then CBN evaluation of e terminates too (Other direction not true).

	scala> def loop: Int = loop
	scala> def first(x: Int, y: Int) = x
	scala> first(1, loop)
		CBN: 1
		CBV: Infinite recursive call

	scala> def constOne(x: Int, y: => Int) = 1
	scala> val const = constOne(1+2, loop)
		constOne(3, loop)
		val const: Int = 3

	scala> val const = constOne(loop, 1+2)
		constOne(loop, 1+2)
		constOne(loop, 1+2)
		...................
		val const: Int = WARNING (Infinite recursive call)

#### Practice Quiz 1.3

	1) In Scala
		
		- all-by-value is the default

			Correct
		
		- if the type of a function parameter starts with a =>, then it uses call-by-value
		
		- call-by-name is not possible
		
		- call-by-value is not possible
		
		- if the type of a function parameter starts with a =>, then it uses call-by-name

			Correct, In `def constOne(x: Int, y: => Int) = 1', 'y' is a call-by-name parameter
		
		- call-by-name is the default

	2) To introduce a definition evaluated only when it is used, we use the keyword

		- def

			Correct, 'def' introduces a definition where the right hand side is evaluated on each use

		- val

## Lecture 1.4 - Conditionals and Value Definitions

Scala has conditional expression if-then-else

Turnary definitions dont exist in scala must use if-then-else:

	scala> def abs(x: Int) = if x>= 0 then x else -x
		x >= 0 is a predicate of type Boolean

Boolean Expressions b can be composed of:

	true false 	// Constants
	!b		// Negation
	b && b		// Conjunction
	b || b		// Disjunction

and of unual comparision op[erations:

	e <= e, e >= e, e < e, e > e, e == e, e != e

Rewrite rules for Booleans (Here are reduction rules for Boolean expressions (e is an arbitrary expression)):

	!true		-> 		false
	!false		-> 		true
	true && e	-> 		e
	false && e	-> 		false
	true || e	-> 		true
	false || e	-> 		e

**Like javaScript, && and || do not always need their right operand to be evaluated. Known as a "short-circuit evaluation"**

### Value Definitions

Function parameter can be passed by value or be passed by name. The same distiction applies to definitions. 

The def form is "by-name," its right hand side is evaluated on each use. There is also a val form, which is "by-value." Example:

	scala> val x = 2
	scala> val y = square(x)

The right-hand side of a val definition is evaluated at the point of the definition itself. 
Afterwards, the name refers to the value. For instance, y above refers to 4, not square(2)


#### Value Definitions and Termination

The difference between val and def becomes apparent when the right hand side of the loop does not terminate. Given, 

	scala> def loop: Boolean = loop

A definition is okay

	scala> def x = loop

A val will lead to an infinite loop

	scala> val x = loop

#### Exercise 1.4.1

Write functions and and or such that for all argument expressions x and y:

	and(x, y) == x && y
	scala> def andComp(x: Boolean, y: => Boolean): Boolean = if x then y else false
	scala> def loop: Boolean = loop
	scala> val passingLoop = andComp(false, loop)
	val passingLoop: Boolean = false

	or(x, y) == x || y
	scala> def orComp(x: Boolean, y: Boolean): Boolean = if(x) then true else if(y) then true else false

## Lecture 1.5 - Example: square roots with Newton's method

Calculate the square root of parameter x 

	scala> def sqrt(x: Double): Double = ...

### Classical Approach

If squared guess is in neighborhood of 1/1000 of the number we're trying to find square root, terminate guess

	scala> def isGoodEnough(guess: Double, x: Double): Boolean = if  math.abs(guess*guess-x) < 0.001 then true else false

Find halfway point between values

	scala> def improve(guess: Double, x: Double): Double =  (guess + x/guess)/2

Newton's Approximation

	scala> def sqrtIter(guess: Double, x: Double): Double = {
			if isGoodEnough(guess, x) then guess
			else sqrtIter(improve(guess, x), x)
		}
	scala> def sqrt(x: Double) = sqrtIter(1.0, x)

#### Exercise 1.5

	1) The isGoodEnough subroutine is not precise for small numbers, leading to non-termination for very large numbers. Why?

		very large numbers can not be squared in the specified bit range

	2) Design a different version of isGoodEnough that does not have these problems

		scala> def isGoodEnough(guess: Double, x: Double): Boolean = if math.abs(guess - x / guess) < 0.001 then true else false

	3) Evaluate new isGoodEnough subroutine in context

		scala> sqrt(0.001)
		scala> sqrt(1.0e-21)
		scala> sqrt(1.0e20)
		scala> sqrt(1.0e50)

#### Practice Quiz 1.5

	1) Recursive functions
		- are functions which include calls to themselves in their definition

			Correct, The 'sqrtIter' function in Lecture 7 contains a call to itself, it is a recursive function
		
		- always terminate
		
		- require an explicit return type in Scala
		
			Correct, the return type can be omitted in non-recursive functions 
			but it is required by the compiler for recursive functions
		
		- are introduced by a dedicated keyword

## Lecture 1.6 - Blocks and Lexical Scope

If use function you dont access directly outside of another function (helper/auxciliary function), 
its best practice to avoid "name space pollution," and put them inside the function where they are referenced: 
process is known as **Nesting**.

	scala> def sqrt(x: Double) = {
			def sqrtIter(guess: Double, x: Double): Double = {
				if isGoodEnough(guess, x) then guess
				else sqrtIter(improve(guess, x), x)
			}

			// Auxciliary Functions
			def isGoodEnough(guess: Double, x: Double): Boolean = if abs(square(guess) - x)<0.001 then true else false
			def improve(guess: Double, x: Double): Double =  (guess + x/guess)/2

			// Helper of Auxciliary Functions
			def abs(x: Double): Double = if x>=0 then x else -x
			def square(x: Double): Double = x*x

			sqrtIter(1.0, x)
		}

### Blocks in Scala

A block is delimited by braces {...}

	scala> {def f(y: Int) = y+1
		val result = 
			val x = f(3)
			x*x
		}
		val result: Int = 16

Contains a sequence of definitions or expressions. The last element of a block is an expression that defines its value. 
Blocks are themselves expressions and can appear anywhere. 
Scala 3 braces are optional around correctly indented expression that appear after =, then, else, ...

	scala> def sqrt(x: Double) =
			def sqrtIter(guess: Double, x: Double): Double =
				if isGoodEnough(guess, x) then guess
				else sqrtIter(improve(guess, x), x)

			// Auxciliary Functions
			def isGoodEnough(guess: Double, x: Double): Boolean = if abs(square(guess) - x)<0.001 then true else false
			def improve(guess: Double, x: Double): Double =  (guess + x/guess)/2

			// Helper of Auxciliary Functions
			def abs(x: Double): Double = if x>=0 then x else -x
			def square(x: Double): Double = x*x

			sqrtIter(1.0, x)

Definitions inside a block are only visible from within the block. 
The definitions inside a block show definitions of the same names outside the block

	scala> {val x = 0
		def f(y: Int) = y+1
		val y = 
			val x = f(3)
			x*x
		val result = y+x
		}
		val result: Int = 16

### Lexical Scoping

Definition: outer blocks are visible inside a block unless they are shadowed. 
Parameters act as global references within their own block.

Therefore we can simplify sqrt by eliminating redundant occurrences of the parameter, which means everywhere the same thing:

	scala> def sqrt(x: Double) =
			def sqrtIter(guess: Double): Double =
				if isGoodEnough(guess) then guess
				else sqrtIter(improve(guess))

			// Auxciliary Functions
			def isGoodEnough(guess: Double): Boolean = if abs(square(guess) - x)<0.001 then true else false
			def improve(guess: Double): Double =  (guess + x/guess)/2
 Assignment: Recursion
			// Helper of Auxciliary Functions
			def abs(x: Double): Double = if x>=0 then x else -x
			def square(x: Double): Double = x*x

			sqrtIter(1.0)

### Semicolons

If there is more than one statement on a line, they need to be separated by semicolons

	val y = x+1; y*y

Similar to javaScript, could put semicolons at the end of everyline, but this would be extranious

	val x = 1;

#### Practice Quiz 1.6

	1) Which statement is true about the following snippet?

		def f =
			val x = 3
			val y =
		    		val xPlusOne = x + 1
		    		xPlusOne * xPlusOne
		  	def g(arg: Int): Int =
		    		// POSITION 1
		// POSITION 2

		- At 'POSITION 2', it is possible to invoke the function 'g'

		- At 'POSITION 1' it is possible to access xPlusOne

		- At 'POSITION 1' it is possible to read the value of 'x'

			Correct, both 'x' and 'g' are defined inside the same block and 'g' is defined after 'x'. 
			Inside the body of 'g' it is possible to access all variables defined before 'g' in 'f'

## Lecture 1.7 - Tail recursion

One Simple rule: One evaluates a function application $f(e_1, ..., e_n)$

- by evaluating the expressions $e_1, ..., e_n$ resulting in the values $v_1, ..., v_n$, then
- by replacing the application with the body of the function $f$, in which
- the actual parameters $v_1, ..., v_n$ replace the formal parameters of $f$

### Application Rewriting Rule

This can be formalized as a rewriting of the program itself:

>def $f(x_1, ..., x_n)$ = B; ... $f(v_1, ..., v_n)$

>def $f(x_1, ..., x_n)$ = B; ... \[$v_1/x_1, ..., v_n/x_n$\] B

Here,  \[$v_1/x_1, ..., v_n/x_n$\] B means:

The expression B in which all occurances of $x_i$ have been replaced by $v_i$.  \[$v_1/x_1, ..., v_n/x_n$\] is called a **Subsitution**

#### Rewriting Example:

Consider gcd, the function that computes the greatest common divisor of two numbers.
The Euclidean algorithm's implemenmtation of gcd

	scala> def gcd(a: Int, b: Int): Int = if b==0 then a else gcd(b, a%b)
	scala> val greatest = gcd(14, 21)
		if 21==0 then 14 else gcd(21, 14%21)
		if false then 14 else gcd(21, 14%21)
			gcd(21, 14)
		if 14==0 then 21 else gcd(14, 21%14)
		if false then 21 else gcd(14, 21%14)
			gcd(14, 7)
		if 7==0 then 14 else gcd(7, 14%7)
		if false then 14 else gcd(7, 14%7)
			gcd(7, 0)
		if 0==0 then 7 else gcd(0, 7%0)
		val greatest: Int = 7
	
Consider factorial:

	scala> def factorial(n: Int): Int = if n==0 then 1 else n*factorial(n-1)
	scala> val reduction = factorial(4)
		if 4==0 then 1 else 4*factorial(4-1)
		if false then 1 else 4*factorial(4-1)
			4*factorial(3)
		4*(if 3==0 then 1 else 3*factorial(3-1))
		4*(if false then 1 else 3*factorial(3-1))
			4*3*factorial(2)
		4*3*(if 2==0 then 1 else 2*factorial(2-1))
		4*3*(if false then 1 else 2*factorial(2-1))
			4*3*2*factorial(1)
		4*3*2*(if 1==0 then 1 else 1*factorial(1-1))
		4*3*2*(if false then 1 else 1*factorial(1-1))
			4*3*2*1*factorial(0)
		4*3*2*1*(if 0==0 then 1 else 0*factorial(0-1))
		4*3*2*1*(if false then 1 else 0*factorial(0-1))
		4*3*2*1*1
		24

What are differences between the two sequences?

GCD has a recursive call as its last reduction step. 
Factorial has a layer of nesting because at each level it not only has to perform the recursive call to 
factorial, but also has to multiply by some constant.

**Implementation Consideration:** If a function calls itself as its last action, the function's stack frame can be reused. 
This is called tail recursion.

=> Tail recursive functions are iterative processes

In general, if the last action of a function consists of calling a function (which may be the same), one stack frame would be sufficient for both functions. Such calls are called tail-calls.

#### Tail Recursion in Scala

Scala can only use tail recursion to optimize functions with direct recurive calls (gcd example).
To impliment such a function you must first:

	scala> import scala.annotation.tailrec
	scala> @tailrec
		def gcd(a: Int, b: Int): Int = if b==0 then a else gcd(b, a%b)

If you try this method with a function thats not tail recursive, an error will be issued.
This will help the runtime and can reduce stack overflows (normally get 2-3K recursive calls with JVM before an overflow)

#### Exercise 1.7

Design a tail recursive version of factorial:

	scala> def factorial(n: Int): Int = 
			@tailrec
			def loop(a: Int, acc: Int): Int = 
				if a==0 then acc 
				else loop(a-1, a*acc)
			loop(n, 1)

#### Practice Quiz 1.7

	1) A tail recursive function

		- calls itself as its last action
		
			Correct, the last call of the function to itself is called tail call
		
		- can be optimized by reusing the stack frame
		
			Correct, this is the main advantage of preferring tail recursive functions:
			the memory overhead is reduced and the number of iterations is not limited by the stack size
		
		- represents an iterative process
		
			Correct, tail recursive functions represent processes where the same procedure is repeated with 
			modified inputs such as the greatest common divisor
		
		- can be annotated with @tailrec so that the compiler will succeed only if it can verify that the function is indeed tail recursive
		
			Correct, The annotation verifies that a tail recursive function contains a tail-call to itself

