# Lazy Evaluation
Only computing results when they are needed, similar to a call by name expression, but with values.
Also extend structural induction to trees

## Lecture 2.1 - Structural Induction on Trees
Will use structural induction on trees to prove laws about them.
Not limited to lists; it applies to any tree structure.
TO prove a property $P(t)$ for all trees $t$, NTS:
- $p(1)$ holds for all leaves 1 of a tree
- for each type of internal node $t$ with subtrees $s_1$,...,$s_n$ show $P(s_1)$^...^$P(s_n)$ implies P(t)

In words: prove for all leaves, then assume all subtrees to prove tree.

### IntSets example

	scala> abstract class Intset:
		include(x: Int): IntSet
		contains(x: Int): Boolean
	scala> object Empty extends IntSet
		include(x: Int): IntSet = NonEmpty(x, Empty, Empty)
		contains(x: Int): Boolean = false
	scala> case class NonEmpty(elem: Int, left: IntSet, Right: IntSet) extends IntSet:
		include(x: Int): IntSet = 
			if x < elem then NonEmpty(elem, left.include(x), right)
			else if x > elem NonEmpty(elem, left, right.include(x))
			else this			
		contains(x: Int): Boolean = 
			if x < elem then left.contains(x) 
			else if x > elem right.contains(x)
			else true
	
#### Laws of IntSet
Need to prove the three laws of IntSet to prove this implementation is "Correct".
Naimly: for any set $s$, and elements $x$ and $y$:

	Empty.contains(x) = false
	s.include(x).contains(x) = true
	s.include(y).contains(x) = s.contains(x), if x!=y

**Prop 1:** Empty object will always return false

**Prop 2:** 
- Basecase: Empty.inlcude(x).contains(x) => NonEmpty(x, Empty, Empty).contains(x) => true
- Inductive Step: Assume `s.include(x).contains(x) = true` holds for all subtrees of s, NTS, holds for s 
case1: NonEmpty(y, left, right).include(y) => NonEmpty(y, left,  right).contains(y) -> true
case2: NonEmpty(x, left, right).include(y) => if x>y then  NonEmpty(x, left.include(y), right).contains(y) else  NonEmpty(x, Empty,  right.include(y)).contains(y)
=> NonEmpty(x,  left.include(y), Empty).contains(y) -> left.include(y).contains(y) -> true || 
NonEmpty(x, left,  right.include(y)).contains(y) -> right.include(y).contains(y) -> true

**Prop 3:**
- Basecase: Empty.include(x).contains(y) => NonEmpty(x, Empty, Empty).contains(y) => Empty.contains(y) => false
- InductiveStep: Assume `s.include(y).contains(x) = s.contains(x), if x!=y` holds for all subtrees of s, and y/x are elements of s: NTS, holds for s
case1: NonEmpty(y, left, right).include(y).contains(x) => NonEmpty(y, left, right).contains(x) = NonEmpty(y, left, right).contains(x)
case1.5: NonEmpty(x, left, right).include(y).contains(x) => 
	if x > y NonEmpty(x, left.include(y), right).contains(x) else NonEmpty(x, left, right.include(y)).contains(x) => true = NonEmpty(x, left, right).contains(x)
case2: NonEmpty(elem, left, right).include(y).contains(x) => 
	if elem < y then  NonEmpty(elem, left, right.include(y)).contains(x) => right.include(y).contains(x) 
		(inductive hypo) => right.contains(x) = NonEmpty(elem, left, right).contains(x)
	else NonEmpty(elem, left.include(y), right).contains(x)
		if x > elem then right.contains(x) = NonEmpty(elem, left, right).contains(x)
		else left.include(y).contains(x) (inductive hypo) => left.contains(x) = NonEmpty(elem, left, right).contains(x)

#### Exercise 2.1.1
Prove the `union` subroutine satisfies laws where:
	scala> object Empty Extends IntSet:
		def union(that: InSet): IntSet = that
	scala> case class NonEmpty(elem: Int, left: IntSet, right: IntSet) extends IntSet:
		def union(that: IntSet): IntSet = left.union(right.union(that)).include(x)

**Prop 4:** xs.union(ys).contains(x) = xs.contains(x) || ys.contains(x)
- Basecase: Empty.union(ys).contains(x) => ys.contains(x) || Empty.contains(x)
- InductiveStep: Assume ` xs.union(ys).contains(z) = xs.contains(z) || ys.contains(z)` holds for all subtrees of xs: NTS, holds for xs
	(*) LHS == leftxs.union(rightxs.union(ys)).include(x).contains(z)
		case1 (x == z): (By Law 2), (*) == true == xs.contains(z) || ys.contains(z)
		case2 (x != z: (By Law 3), (*) == leftxs.union(rightxs.union(ys)).contains(z) ==
			(By Inductive Hypothesis) leftxs.contains(z) || rightxs.union(ys).contains(z) ==
			(By Inductive Hypothesis) leftxs.contains(z) || rightxs.contains(z) || ys.contains(z) ==
			xs.contains(z) || ys.contains(z)

## Lecture 2.2 - Lazy Lists
### Collections and Combinatorial Search
We've seen a number of immutable collections that provide powerful operations, in particular for combinatorial search.

For instance, to find the second prime number between 1000 and 10000, `(1000 until 10000).filter(isPrime)(1)`

**Performance Problem:** This subroutine is not ideal though because it constructs all primes between 1000 and 10000, only using 2 elements from this list.
By reducing the upper bound we can speed things up, but risk missing the second prime number all together

### Delayed Evaluation
can avoid this computation by using a trick: *avoid computing elements of list until they are needed for evaluation result*

This idea is implemented in `LazyList`, similar to lists but their elements are only evaluated on demand

#### Defining Lazy Lists
defined based on two components, LazyList.empty, and a LazyList.cons constructor

	scala> val xs = LazyList.cons(1, LazyList.cons(2, LazyList.empty))

Can also edfine other collections by using the object `LazyList` as a factory

	scala> LazyList(1, 2, 3)

We can also use the method `to(LazyList)`, to is a utility method that takes an object (which is a factory) and causes the apply method on that object, "casting to something"

	scala> (1000 until 10000).to(LazyList)

Supports almost all methods available to Lists
- cons operator, ::, always produces a lazylist
- x #:: xs == LazyList.cons(x, xs), always produces a lazy list

### LazyList Ranges
write a functions that return (lo until hi).to(LazyList) 

	scala> def lazyRange(lo: Int, hi: Int): LazyList[Int] = 
		if lo >= hi then LazyList.empty
		else LazyList.cons(low, lazyRange(lo + 1, hi))
	scala> def listRange(lo: Int, hi: Int): List[Int] = 
		if lo >= hi then Nil
		else low :: listRange(lo + 1, hi))


### Comparing Two Range Fucntions
- listRange will construct all elements of the range in an instantaited List
- lazyRange will construct a single object, typed LazyList, pointing to each element
- elements are only computed when needed like a head or tail (!!) on the lazy list 

### Implementation of Lazy Lists
Implememntation is quite subtle. For simplification, consider lazy lists are only lazy in their head, tail and isEmpty are computed when the lazy list is created.
Not actaul behavior, but makes implementation simpler to understand. The trai LazyList would look like

	scala> trait TailLazyList[+A] extends Seq[A]:
		def isEmpty: Boolean
		def head: A
		def tail: TailLazyList[A]
		...

As with list other methods can be defined in terms of these three

	scala> object TailLazyList:
		def cons[T](hd: T, t1: => TailLazyList[T]) = new TailLazyList[T]:
			def isEmpty = false
			def head = hs
			def tail = t1
			override def toString = "LazyList("+hs+")"
	scala> val empty = new TailLazyList[Nothing]
			def isEmpty = true
			def head = throw NoSuchElementException("empty.head")
			def tail = throw NoSuchElementException("empty.tail")
			override def toString = "LazyList()"
		
The only important difference between the implementations of List and (simplified) LazyList concern the tail argument; 
namely each time you need to reference the tail, since its a call by name parameter you'll need to recompute that value.

### Exercise 2.2.1
Consider the modifications of LazyRange

	def lazyRange(lo: Int, hi: Int): TailLazyList[Int] =
		print(lo+" ")
		if lo >= hi then TailLazyList.empty
		else TailLazyList.cons(lo, LazyRange(lo + 1, hi))

When you write lazyRange(1,10).take(3).toList what gets printed => 1 2 3

## Lecture 2.3 - Lazy Evaluation
The tail problem can be avoided by storing the result of the first evaluation of tail and reusing the stored result instead of recomputing tail.
Call this *scheme* lazy evaluation (as opposed to **by-name evaluation** in the case where everything is recomputed, and **strict evaluation** for normal parameters and 
val definitions)

### Lazy Evaluation
Some functional programming languages (like Haskell), use lazy evaluations by default. Scala uses strict by default, but allows for lazy definitions using `lazy val x = expr`.
So the righthand side is only computed when the x is used

### Exercise 2.3.1
Consider the following Program

	scala> def expr = 
		val x = { print("x"); 1}
		lazy val y = { print("y"); 2}
		def z = { print("z"); 3}
		z+y+x+z+y+x

If called what gets printed as a side effect => xzyz

### Lazy Vals and Lazy Lists
Using a lazy value for `tail`, `TailLazyList.cons` can be implemented more efficiently

	def cons[T](hd: T, tl: LazyList[T]) = new TailLazyList[T] =
		def head = hd
		lazy val tail = tl
		...

**Seeing it in Action:** To convince ourselves that the implementation of lazy lists really does avoid unnecessary computations, let's observe the execution trace of the expression

	lazyRange(1000, 10000).filter(isPrime).apply(1)
	
### Seeing it in Action
To convince ourselves that the implementation of lazy lists really does avoid unnecessary ocmputations, let's  observe the execution trace of the expression:

	scala> lazyRange(1000, 10000).filter(isPrime).apply(1)
	-> (if 1000 >= 10000 then empty
		else cons(1000, lazyRange(1000 + 1, 10000)).filter(isPrime).apply(1)
	-> cons(1000, lazyRange(1000 + 1, 10000)).filter(isPrime).apply(1)
	(*) == cons(1000, lazyRange(1000 + 1, 10000))
	-> (*).filter(isPrime).apply(1) 
	-> (if (*).isEmpty then (*)
		else if isPrime((*).head) then cons((*).head, (*).tail.filter(isPrime))
		else (*).tail.filter(isPrime).apply(1)
	-> if isPrime(1000) then cons((*).head, (*).tail.filter(isPrime))
		else (*).tail.filter(isPrime).apply(1)
	-> if false then cons((*).head, (*).tail.filter(isPrime))
		else (*).tail.filter(isPrime).apply(1)
	-> (*).tail.filter(isPrime).apply(1)
	-> lazyRange(1001, 10000).filter(isPrime).apply(1)
	...
	-> lazyRange(1009, 10000).filter(isPrime).apply(1)
	-> cons(1009, lazyRange(1009 + 1, 10000)).filter(isPrime).apply(1)
	-> cons(1009, lazyRange(1009 + 1, 10000)).apply(1) 
	-> cons(1009, cons(1009, lazyRange(1009 + 1, 10000)).tail.filter(isPrime)).apply(1) 

Assume Apply defined like this in LazyList[T]

	scala> def apply(n: Int): T =
		if n==0 then head
		else tail.apply(n-1)

Then

	-> if 1 == 0 then cons(1009, cons(1009, lazyRange(1009 + 1, 10000)).tail.filter(isPrime)).head
		else cons(1009, cons(1009, lazyRange(1009 + 1, 10000)).tail.filter(isPrime)).apply(0)
	-> false
	-> cons(1009, cons(1009, lazyRange(1009 + 1, 10000)).tail.filter(isPrime)).apply(0)
	-> cons(1009, cons(1009, lazyRange(1009 + 1, 10000)).tail.filter(isPrime)).apply(0)
	-> lazyRange(1009 + 1, 10000)).tail.filter(isPrime).apply(0)
	...
	-> if 0 == 0 then cons(1013, cons(1013, lazyRange(1012 + 1, 10000)).tail.filter(isPrime)).head
		else cons(1013, cons(1013, lazyRange(1012 + 1, 10000)).tail.filter(isPrime)).apply(0)
	-> true
	-> cons(1013, cons(1013, lazyRange(1012 + 1, 10000)).tail.filter(isPrime)).head
	-> 1013	

### RealWorld Lazy List
The simplified implementation shown fro `LazyList` has a lazy `tail`, but not a lazy `head` nor a lazy `isEmpty`.
The real implementation is lazy for all three operations.
To do this maintain a lazy state variable, like this:

	scala> class LazyList[+T](init: => State[T]):
		lazy val state: State[T] = init
	scala> enum State[T]
		case Empty
		case Cons(hd: T, tl: LazyList[T])

## Lecture 2.4 - Computing with Infinite Sequences
### Infinite Lists
You saw that elements of a lazy list are only ocmputed when they are needed to produce aresult. This opens up the possibility ot define infinite lists.
For instance, here is the (lazy) list of all integers starting from a given number:

	def from(n: Int): LazyList[Int] = n #:: from(n+1)

The list of all natural numbers

	val nats = from(0)

Lists of all multiples of 4

	nats.map(_*4)

### The Sieve of Eratosthenes
An ancient technique to calculate primes:
- Start with all integers from 2, the first prime number
- Eliminate all multiples of 2
- The first element of the resulting list is a 3, a prime number
- Eliminate all multiples of 3
- Iterate forever

	scala> def sieve(s: LazyList[Int]): LazyList[Int] =
		s.head #:: sieve(s.tail.filter(_%s.head != 0))
	scala> val primes = sieve(from(2))

To see the first N prime numbers:

	scala> primes.take(N).toList

### Back to Square Roots
Lazy list are useful to define compents that can be put together in very flexible ways.
We can now express the concept of a converging sequence without having to worry about when to terminate it

	scala> def sqrtSeq(x: Double): LazyList[Double] =
		def improve(guess: Double) = (guess + x / guess) /2
		lazy val guesses: LazyList[Double] = 1 #:: guesses.map(improve)
		guesses

Must have guesses as a alzy val, otehrwise type error where geusses is a forward reference extending over the definition of guesses.
Scala has builtin checks that make sure you dont have a recursive or mutually recursive or forward definition of a val in a local list.
In principle its okay, but the compiler is overly conservative.

#### Termination
can add `isGoodEnough` later

	scala> define isGoodEnough(guess: Double, x: Double) =
		math.abs((guess*guess -x)/x) < 0.0001
	scala> sqrtStep(4).filter(isGoodEnough(_, 4)).head

This process was used as the opening argument for a paper called "Why Functional Programming Matters" by John Hughes

### Exercise 2.4.1
Consider two ways to express the infinite list of multiples of a given number N

	scala> val xs = from(1).map(_*N)
	scala> val ys = from(1).map(_%N 0= 0)

`xs` is faster because every element of the original list will immediately contribute to the list, whereas the second list generates more elements that are disgarded by the filter

## Lecture 2.5 - Case Study: the Water Pouring Problem
- You are given some glasses of different sizes
- You task is to produce a glass with a given amount of water in it
- You can do so by measure or balance
- All you can do is
 - fill a glass (completely)
 - empty a glass
 - pour from one glass to another until the first glass is empty or the second glass is full

**Example:** You have two glasses. One hold 7 units of water, the other 4. Produce a glass filled with 6 units of water
- give 4oz glass index 0 and 7oz glass index 1

Then we have 2 case either 
- empty 0
- empty 1
- fill 0
 - pour 0 -> 1
  - fill 0
   - pour 0 -> 1
    - empty 1
...
- fill 1
...

*Idea:* create a lazy data structure that computes all possible paths and pick one that ends in the correct target state.
How can we avoid exploring a subspace of the search space that doesn't lead to a solution?
Do this by first producing shorter paths before longer paths- as to eliminate subpaces sooner.
Essentailly produce all paths of length 1, length 2, ..., until solution then return paths.
Second thing to watch out for is cycles, dont want  to return to the same state, or any previous state.
To avoid this we will keep track of what target states have been reached in each subspace and if a path leads to one of the previous target states,
then we'll disgard that path (edge of graph)

### States and Moves
Representations
- Glass: Int (glasses are numbered 0, 1, 2)
- State: Vector[Int] (one entry per glass)

So the vector[2, 3] would be the state where we have two glasses that have 2 and 3 units of water in it.

Moves:
- Empty(glass)
- Fill(glass)
- Pour(from, to)

	type Glass = Int
	type State = Vector[Int]


	class Pouring(full: State):
		// Telling how to apply a move
		enum Move:
			case Empty(glass: Glass)
			case Fill(glass: Glass)
			case Pour(from: Glass, to: Glass)

			def apply(state: State): State = this match
				case Empty(glass) => state.updated(glass, 0) 
				case Fill(glass) => state.updated(glass, full(glass))
				case Pour(from, to) => 
					val amount = state(from) min full(to) - state(to)
					state.updated(from, state(from) - amount).updated(to, state(to) + amount)

		end Move

		// Giving all possible paths
		val moves =
			val glasses: Range = 0 until full.length
			(for g <- glasses yield Move.Empty(g))
			++ (for g <- glasses yield Move.Fill(g))
			++ (for g1 <- glasses; g2 <- glasses if g1!=g2 yield Move.Pour(g1, g2))

		// Record paths in reverse order, want history or the last move comes first and initial empty would come last
		class Path(history: List[Move], val endState: State):
			def extend(move: Move) = Path(move :: history, move(endState))
			override def toString = s"${history.reverse.mkString(" ")} --> $endState"

		end Path

		// Search Algorithm, here pathFrom metho is similar to from method on the set of natural numbers
		// Takes an initial list of paths, initial set of states already encountered
		// => all valid lists that start in these paths, where pathFrom start at empty list and are ordered by length e.g. Empty, Paths.length==1,...
		def pathFrom(paths: List[Path], explored: Set[State]): LazyList[List[Path]] =
			// Set of paths extended by 1
			val frontier = 
				for // Iterating over all paths and moves
					path <- paths
					move <- moves
					next = path.extend(move)
					if !explored.contains(next.endState) // If next move is not cyclic
				yield next // Accept the next move
			// Append new paths to 
			paths #:: pathFrom(frontier, explored ++ frontier.map(_.endState))

		val empty: State = full.map(x => 0)
		val start = Path(Nil, empty)

		// Find which paths lead to a solution, contains a lazy list of paths (infinite) that end in target state
		def solutions(target: Int): LazyList[Path] =
			for
				paths <- pathFrom(List(start), Set(empty)) // construct serach space to iterate over
				path <- paths
				if path.endState.contains(target) // Pick paths that end in state such that there is a glass with target unit in it
			yield path // lazy list of paths where every path is a solution to original problem
	 end Pouring

	
	val problem = Pouring(Vector(4,7))
	problem.solutions(6).head

### Variants
In a program of the complexity of the pouring program, there are many choices to be made
- Specific classes for moves and paths, or some encoding
- Object Oriented methods, or naked data structures with functions

This is just one solution, though its not necessarily the shortest

### Guiding Principles for Good Design
- Name everything you can
- Put operations into natural scopes
- Keep degrees of freedom for future refinements
























