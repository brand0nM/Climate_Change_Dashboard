# Lists
Finally you'll lean about structural induction, which is a powerful method that can prove the correctness of algorithms 
over lists and other recursive data structures.

## Lecture 5.1 - A Closer Look at Lists
### List Recap
List are the core data structure we will work with over the next weeks.

**Type:** List[Fruit]

**Construction:**

	scala> val fruits = List("Apple", "Pears","Banana")
	scala> val fruits2 = "Apple" :: "Pears" :: "Banana" :: Nil

** Decomposition:**

	scala> fruits.head                     // "Apple"
	scala> fruits.tail                     // 2 :: Nil
	scala> fruits.isEmpty                  // false
	scala> val nums = 1 :: 2 ::  Nil

	scala> nums match
		case x :: y :: _ => x + y      // 3

### List Methods
- xs.length: The number of elements of xs
- xs.last: The list's last element, exception if xs is empty
- xs.init: A list consisiting of all elements of xs except the last one, exception if xs is empty
- xs.take(n): A list consisting of the first elements of xs, or xs itself if it is shorter than n
- xs.drop(n): The rest of the collection after taking n elements
- xs(n): formally xs.apply(n), takes the nth - 1 element of a list

#### Create New Lists
- xs ++ ys: The list of all elements of xs followed by all elements of ys
- xs.reverse: The list containing the elements of xs in reversed order
- xs.updated(n, x): The list containing the same elements as xs except at index n where it contains x

#### Finding Elements
- xs.indexOf(x): The index of the first eleemnt in xs equal to x, or -1 if x does not appear in xs
- xs.contains(x): Same as indexOf method, where it's >= 0, or exists

### Implementation of Last
The complexity of `head` is (small) constant time. What is the complexity of `last`?

Write a possible implementation of last as a stand-alone function

	scala> def last[T](xs: List[T]): T = xs match
		case List() => throw Error("last of empty list")
		case List(x) => x
		case y :: ys => last(ys)

so `last` takes steps proportional to the `length` of the list `xs`

#### Exercise 5.1.1
Implement `init` as an external function, analogous to `last`

	scala> def init[T](xs: List[T]): List[T] = xs match
		case List() => throw Error("init of empty list")
		case List(x) => List()
		case y :: ys => y :: init(ys)
		
### Implementation of Concatenation
How can `concat` be implemented. Try writing an extension method for `++`:

	scala> extension[T](xs: List[T])
		def ++ (ys: List[T]): List[T] = xs match
			case Nil => ys
			case x :: xs1 => x :: (xs1 ++ ys) 

What is the time complexity of this subroutine? 
> **Answer:** O(xs.length) ~ $n$, for lists of length n

### Implementation of Reverse 
How can `reverse` be implemented. Try writing an extension method for `reverse`:

	scala> extension[T](xs: List[T])
		def reverse: List[T] = xs match
			case Nil => Nil
			case y :: ys => ys.reverse ++ List(y)

What is the time complexity of this subroutine? 
> **Answer:** O(xs.length ** xs.length) ~ $n$, for lists of length $n^2$

#### Exercise 5.1.2
Remove the n'th element of a list `xs`. If n is out of bounds, return `xs` itself.
`removeAt(1, List('a','b','c','d'))` -> `List(a,c,d)`

	scala> def removeAt(n: Int): List[T] = xs match
		case Nil => Nil
		case y :: ys => if y == 0 then ys else y :: removeAt(n-1, ys)

#### Exercise 5.1.2
Flatten a list structure
`flatten(List(List(1,1),2,List(3,List(5,8))))` => `List[Any] = List(1,1,2,3,5,8)`

	scala> def flatten(xs: List[T]): List[T] = xs match
		case Nil => Nil
		case y :: ys => flatten(y) ++ flatten(ys)
		case _ => xs :: Nil

#### Practice Quiz 5.1
1) Accessing the last element of a Scala list has

	- cubic time complexity

	- quadratic time complexity

	- constant time complexity

	- time complexity proportional to the length of the list

		Correct, To access the last element of a list, we need to iterate over all the elements of the list once. 
			Therefore the complexity is linear in the size of the list

## Lecture 5.2 - Tuples and Generic Methods
### Sorting Lists Faster
As a non-trivial example, let's design a fucntion to sort lists that is more efficient than insertion sort.

A good algorithm for this **merge sort**. 
The idea is as follows; If the list consisits of zero or one elements, 
it is already sorted.

- Separate the list into two sub-lists, each containing around half of the elements of the original list
- sort the two sub-lists
- Merge the two sorted sub-lists into a single sorted list

### First MergeSort Implementation

	scala> def msort(xs: List[Int]): List[Int] =
		val halfLength = xs.length%2
		if halfLength == 0 then xs
		else
			def merge(xs: List[Int], ys: List[Int]) = ???					
			val (left, right) = xs.splitAt(n)
			merge(msort(left), msort(right))

### Pairs and Tuples
The pair ocnsisiting of x and y is written (x, y) in Scala.
`val pair = ("answer", 42)` => `pair: (String, Int) = (answer, 42)`

The type of pair above is `(String, Int)`.
Pairs can also be used as patterns:
`val (lbael, value) = pair` => `label: String = answer, value: Int = 42`

This works analogously for tuples with more than two elements.


### The SplitAt Function
The splitAt function on lists returns two sublists
- the elements up to the given index
- the elements from that index

The lists are returned in a **pair**

	scala> def splitAt(n: Int) = (xs.take(n), xs.drop(n))

### Translation of Tuples
For small(*) $n$, the tuple type $(T_1, ..., T_n)$ is an abbreviation of the parameterized type
`scala.Tuplen[T1, ..., Tn]`. 

A tuple expression $(e_1, ..., e_n)$ is equivalent to the function application `scala.Tuplen(e1, ..., en)`

A tuple expression $(p_1, ..., p_n)$ is equivalent to the function application `scala.Tuplen(p1, ..., pn)`

(*) Scala2 "small," n <= 22. There's also a TupleXXL class that handles Tuples larger than that limit

#### The Tuple Class
Here, all `Tuplen` classes are modeled after the following pattern:

	scala> case class Tuple2[T1, T2] (_1: +T1, _2: +T2):
		override def toString = "(" + _1 + "," + _2 + ")"

The fields of tuple can be accessed with names _1, _2, ...

So instead of the pattern binding `val (label, value) = pair`. Once could have also written 
`val label = pair._1; val value = pair._2`, but the pattern matchin gform is generally preferred.

### Definition of Merge
Here is a definition of the merge function:

	scala> def merge(xs: List[Int], ys: List[Int]) = (xs, ys) match
		case (Nil, ys) => ys
		case (xs, Nil) => xs
		case (x :: xs1, y :: ys1) =>
			if x < y then x :: merge(xs1, ys)
			else y :: merge(xs, ys1)

### Making Sort More General
*Problem:* how to parameterize `msort` so that it can be used for lists with elements other than `Int`

	scala> def msort[T](xs: List[T])(lt: (T, T) => Boolean) =
		val halfLength = xs.length%2
		if halfLength == 0 then xs
		else
			def merge(xs: List[Int], ys: List[Int]) = (xs, ys) match
				case (Nil, ys) => ys
				case (xs, Nil) => xs
				case (x :: xs1, y :: ys1) =>
					if lt(x, y) then x :: merge(xs1, ys)
					else y :: merge(xs, ys1)
					
			val (left, right) = xs.splitAt(n)
			merge(msort(left)(lt), msort(right)(lt))
	scala> msort(List(-5,4,3,2,7))((x, y) => x < y)
	scala> msort(List("apple","pear","orange","pineapple"))((x, y) => x.compareTo(y) < 0)
	
but since, types can be inferred, and strings can be decomposed to bits- which are directly comparable, can also write

	scala> msort(xs)((x, y) => x < y)

#### Practice Quiz 5.2
How can you access the second element of a tuple of two elements? `val pair = ("Scala", 3)`

	scala> val (_, second) = pair
	scala> pair._2		

## Lecture 5.3 - Higher-order list functions
### Recurring Patterns for Computations on Lists
The examples have shown that fucntions on lists often have similar structures. We can identify several recurring patterns like,
- transforming each element in a list in a certain way
- retrieving a list of all elements satisfying a criterion
- combining the elements of a list using operators 

Functional languages allow programemrs to write generic funcrions that implement patterns such as 
these using **higher-order functions**.

### Applying a Funciton to Elements of a List
A common opperation is to transform each elements of a list and then return the list of results

	scala> def scaleList(xs: List[Double], factor: Double): List[Double] = xs match
		case Nil => xs
		case y :: ys => y * factor :: scaleList(ys, factor)

### Mapping
This scheme can be generalized to the method map of `List` class. A simple way to define map is as follows:

	scala> extension[T](xs: List[T])
		def map[U](f: T => U): List[U] = xs match
			case Nil => xs
			case y :: ys => f(x) :: xs.map(f)

Using the method map, scalelist can be written more concisely

	scala> def scaleList(xs: List[Double], factor: Double): List[Double] = xs.map(x => x * factor)

#### Exercise 5.3.1
Consider a  function to square each element of a list and return the result list. Without the map method

	scala> def squareList(xs: List[Double]): List[Double] = xs match
		case Nil => Nil
		case y :: ys => y*y :: squareList(ys)
		
Including the map extension,

	scala> def squareList(xs: List[Double], factor: Double): List[Double] = xs.map(x => x * x)

### Filtering 
Another common operation on lists is the selction of all elements satisfying a given condition. For example:

	scala> def posElems(xs: List[Int]): List[Int] = xs match
		case Nil => xs
		case y: ys => if y>0 then y :: posElems(ys) else posElems(ys)

#### Filter

	scala> def filter(p: T => Boolean): List[Int] = xs match
		case Nil => xs
		case y: ys => if p(y) then y :: ys.filter(p) else ys.filter(p)

#### Variation of Filter
Also the following methods for extracting sublists based on a predicate:

- xs.filterNot(p): Same as xs.filter(x => ! p(x))
- xs.partition(p): Same as (xs.filter(p), xs.filterNot(p))
- xs.takeWhile(p): The longest prefix of list xs consisting of elements that all satify the predicate p
- xs.dropWhile(p): The remainder of the list xs after any leading elements satisfying p have been removed
- xs.span(p): Same as (xs.takeWhile(p), xs.dropWhile(p)), but computes in a single traversal of the list xs

	scala> val nums = List(1,2,3,4,5,6)
	scala> nums.partitions(x => x%2 != 0)        // List(List(1,3,5),List(2,4,6))
	scala> nums.span(x => x%2 != 0)              // (List(1), List(2,3,4,5,6))

#### Exercise 5.3.2
Write a fucntion `pack` that packs consecutive duplicates in seperate sublist
`val elem = List('a','a','a','b','c','c','a')` => `List(List('a','a','a'),List('b'),List('c','c'),List('a'))`

	scala> def pack[T](xs: list[T]): List[List[T]] = xs match
		case Nil => Nil
		csae x :: xs => 
			val (more, rest) = xs.span(y => x=y)
			(x :: more) :: pack(rest)

#### Exercise 5.3.3
Using pack, write a function encode that produces the tun-length encoding of a list.

The idea is to encode n ocnsecutive duplicates of an element x as a pair (x,n). For instance,
`encode(List('a','a','a','b','c','c','a'))` => `List(List('a',3),List('b',1),List('c',2),List('a',1))

	scala> def encode[T](xs: list[T]): List[List[T]] = pack(xs).map(x => (x.head, x.length))

#### Practice Quiz 5.3
1) What is purpose of the 'map' method of 'List'?

	- to select the longest prefix of a list consisting of elements that satisfy a given condition

	- to select all the elements of a list that satisfy a given condition

	- to combine all the elements of a list using an operator

	- to transform each element of a list and return the list of results

		Correct, 'map' takes as a parameter a function and applies it to each element of the list to 
			generate the elements of the list it returns.

			scala> def map[B](f: (A)=> B): List[B] = ...

## Lecture 5.4 - Reduction of Lists
Another common operation on lists is to combine the elements of a list using a given operator.

`sum(List(x1 ,..., xn))` = `0 + x1 + ... + xn`

`product(List(x1 ,..., xn))` = `1 * x1 * ... * xn`

We can implement this with the usual recursive schema:

	scala> def sum(xs: List[Int]): Int = xs match
		case Nil => 0
		case y :: ys => y + sum(ys)

### ReduceLeft
This pattern can be abstracted out using the generic method reduceLeft:

`reduceLeft` insert given binary operator between adjacent elements of a list:

	scala> List(x1, ..., xn).reduceLeft(op) = x1.op(x2). ... .op(xn)

Using reduceLeft, we can simplify

	scala> def sum(xs: List[Int]) = (0 :: xs).reduceLeft((x,y) => x+y)
	scala> def product(xs: List[Int]) = (1 :: xs).reduceLeft((x,y) => x*y)

### A Shorter Way To Write Functions
Instead of ((x, y) => x * y), one can also write shorter: `(_*_)`

Every _ repersents a new parameter, goign from left to right.

The parameters are defined at the next outer pair of parentheses (or the whole expression if there are no enclosing parentheses)

SO, `sum` and `product` can also be expressed like this:

	scala> def sum(xs: List[Int]) = (0 :: xs).reduceLeft(_+_)
	scala> def product(xs: List[Int]) = (0 :: xs).reduceLeft(_*_)

#### FoldLeft
The function `reduceLeft` is defined in terms of a more general function `foldLeft`.

`foldLeft` is like `reduceLeft`, but rakes an **acuuulator** z, as an additional parameter, which is returned when 
`foldLeft` is called on an empty list: `List(x1,...,xn).foldLeft(z)(op) = z.op(x1).op ... .op(xn)`


So sum and product can also be defined as follows
	
	scala> def sum(xs: List[Int]) = xs.foldLeft(0)(_+_)
	scala> def product(xs: List[Int]) = xs.foldLeft(1)(_*_)

!()["Desktop/Scala_Coursera/Unit_5/Pictures/FoldRight_Binary_Tree.pdf"]

### Implementation of ReduceLeft and FoldLeft
`foldLeft` and `reduceLeft` can be implemented in class `List` as follows

	scala> abstract class List[T]

		def reduceLeft(op: (T, T) => T): T = this match
			case Nil => throw IllegalOperationException("Nil.reduceLeft")

		def folLeft[U](z: U)(op: (U, T) => U): U = this match
			case Nil => z
			case x :: xs => xs.foldLeft(op(z,x))(op)

### FoldRight and ReduceRight
Applications of `foldLeft` and `reduceLeft` unfold on trees that lean to the left

They have two dual functions, foldRight and reduceRight, which produce trees which lean to the right i.e.,

`List(x1,...,xn).reduceRight(z)(op) = x1.op(x2.op(...x{n-1}.op(xn)...)`

`List(x1,...,xn).foldRight(op) = x1.op(x2.op(...xn.op(z)...)`

!()["Desktop/Scala_Coursera/Unit_5/Pictures/reduceRight.pdf"]

#### Implementation of FoldRight and ReduceRight
They are defined as follows

	scala> def reduceRight(op: (T,T) => T): T = this match
		case Nil => throw IllegalOperationException("Nil.reduceRight")
		case x :: Nil => x
		case x :: xs => op(x, xs.reduceRight(op))
	scala> def foldRight[U](z: U)(op: (T,U) => U): U = this match
		case Nil => z
		case x :: xs => op(x, xs.foldRight(z)(op))

#### Difference between foldLeft and foldRight
For operators that are associative and commutative, `foldLeft` and `foldRight` are equivalent 
(even though there may be a difference in efficiency)

#### Exercise 5.4.1
Here is another formulation of concat:

	scala> def concat[T](xs: List[T], ys: List[T]): List[T]=
		xs.foldLeft(ys)(_ :: _)

We cant replace `foldLeft` with `foldRight`, because the resulting type does not resolve

!()["Desktop/Scala_Coursera/Unit_5/Pictures/exercise_5.4.1.png"]

### Back to Reversing Lists
we now develop a function for reversing lists which has a linear cost.'

The idea is to use the operation `foldLeft`:

	scala> def reverse[T](xs: List[T]): List[T] = xs.foldLeft(z?)(op?)

All that remains is to replace the parts z? and op?
Let's try to **compute** them from examples.

	scala> def reverse[T](xs: List[T]): List[T] = 
		xs.foldLeft(List[T]())((xs, x)) => x :: xs

#### Exercise 5.4.2
Complete the following defintions of the basic functions `map` and `length` on lists, 
such that their implementation uses `foldRight`

	scala> def mapFun[T, U](xs: List[T], f: T => U): List[U] = 
		xs.foldRight(List[U]())((y, ys) => f(y) :: ys)
	scala> def lengthFun[T, U](xs: List[T], f: T => U): List[U] = 
		xs.foldRight(0)((y, n) => n + 1)

#### Practice Quiz 5.4.3

	scala> def f(ls: List[Int], op: (Int, Int) => Int): Boolean = 
		val a = ls.reduceRight(op)
		val b = ls.reduceLeft(op)

		a == b

1) When does 'f' return 'true'?

	- 'f' returns 'true' if 'op' is commutative.

	- 'f' returns 'true' if 'op' is associative and commutative.

	- 'f' never returns 'true', for any parameters 'ls' and 'op'

	- 'f' returns 'true' for all parameters 'ls' and 'op'.

		Correct, 'reduceRight' can be implemented using 'foldRight' and 'reduceLeft' can be implemented using 
			'foldLeft'. 'foldLeft' and 'foldRight' are equivalent when the operator is commutative and associative.

## Lecture 5.5 - Reasoning about lists
 Want to verify that `++` is associative

	1) (xy ++ ys) ++ zs = xs ++ (ys ++ zs)
	2) xs ++ Nil = xs
	3) Nil ++ xs = xs

### Proof
How can we prove properties like these?

	scala> def ++ (ys: List[T]): List[T] = xs match
		(a) case x :: xs1 => x :: (xs1 ++ ys) 
		(b) case Nil => ys


(a)
basecase: (Nil ++ List(b)) ++ List(c) => List(b) ++ List(c) => Nil ++ (List(b) ++ List(c))

Inductive step: Assume that (x :: xs) then, 

LeftSide:

((x :: xs) ++ ys) ++ zs => (x :: (xs ++ ys)) ++ zs => x :: ((xs ++ ys) ++ zs) => x :: (xs ++ (ys ++ zs)) = (*)

RightSide:
(x :: xs) ++ (ys ++ zs) => x :: (xs ++ (ys ++ zs)) = (*)

(b) Show by induction on `xs` that `xs ++ Nil = xs`

(x :: xs) ++ Nil => x :: (xs + Nil) => x :: xs 

#### Practice Quiz
1) To prove a property 'P' using structural induction on lists, you need to verify:

	- the base case: 'P' holds for the empty list

		Correct, The base case is about proving that 'P' holds for 'Nil'

	- the induction step: 'P' holds for a list containing only one element

	- the base case: 'P' holds for a list containing only one element

	- the induction step: if 'P' holds for a list 'xs', then 'P' holds for 'x :: xs' where 'x' is some element.

		Correct, The induction step in structural induction is similar to the induction step in natural 
			induction. It builds on the base case to prove property 'P'.

	- the base case: if 'P' holds for a list 'xs', then 'P' holds for 'x :: xs' where 'x' is some element.





































































































































