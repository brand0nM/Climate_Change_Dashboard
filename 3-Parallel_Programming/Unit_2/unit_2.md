# Basic Task Algorithms
## Lecture 2.1 - Parallel Sorting
### Merge Sort
1) recursively sort two halves of an array in parallel

2) sequentially merge the two halves by copying to temporary array

3) copy temporary back to original

the `parMergeSort` method takes an array and a maximum depth. 
Signature looks like

	def parMergeSort(xs: Array[Int], maxDepth: int): Unit = ...

#### Allocating the Intermediate array

	val ys = new Array[Int](xs.length)

At each level of merge sort we will alternate between the source array xs and the intermediate array ys.

#### Sorting Routine in parallel

	def sort(from: Int, until: Int, depth: Int): Unit =
		if (depth == maxDepth)
			// historically triggered when reached specific length, now when max depth
			quickSort(xs, from, until - from)
		else
			val mid = (from + until) / 2
			// ensures each disjoint segment of array is sorted- in parallel
			parallel(sort(mid, until, depth + 1), sort(from, mid, depth + 1))

		// used to determine if xs or ys is used as auxillary storage
		val flip = (maxDepth - depth) % 2 == 0
		val src = if (flip) ys else xs
		val dst = if (flip) xs else ys
		// copies elements into new auxillary storage array dst (either xs or ys)
		merge(src, dst, from, mid, until)

	sort(0, xs.length, 0)

#### Merging the Array
Given an array src consisiting of two sorted intervals, merge those intervals into dst array.

	def merge(src: Array[Int], dst: Array[Int], from: Int, mid: Int, until: Int): Unit

The `merge` implementation is sequential, so we will not go through it.

#### Copying the Array

	// copy array src into target, begining seg is from, end is until
	def copy(src: Array[Int], target: Array[Int], 
		from: Int, until: Int, depth: Int): Unit =
		// if base recurssion, start building up from array with source
		if (depth == maxDepth)
			Array.copy(src, from, target, from, until - from)
		else
			val mid = (from + until)/2
			val right = parallel(
				copy(src, target, mid, until, depth + 1)
				copy(src, target, from, mid, depth + 1)
			)
		if (maxDepth % 2 == 0) copy(ys, xs, 0, xs.length, 0)

## Lecture 2.2 - Data Operations and Parallel Mapping
### Parallelism and Collections
Parallel Processsing of collections is important; main application of parallelism today

We examine conditions when this can be done
- properties of collections: ability to split, combine

- properties of operations: associativity, independence

### Functional Programming and Collections
Operations on colelctions are key to functional programming 

**map**: apply funciton to each element
- List(1,3,8).map(x => x*x) == List(1, 9, 64)

**fold**: combine elements with a given operation
- List(1, 3, 8).fold(100)((s,x) => s + x) == 112

**scan**: combine folds all list prefixes
- List(1,3,8).scan(100)((s,x) => s + x) == List(100, 101, 104, 112)

### Choice of Data Structures
We use **List** to specify the results of operations. List are not good for parallel implementations because we cannot efficiently
- split them in half (need to search fro the middle)

- combine them (concat needs linear time)

Alternatives to lists
- Arrays: Imperative (recall array sum)

- trees: can be implemented functionally

Subsequent lectures examine Scala's parallel collection libraries

- includes many more data structures, implemented efficiently

### Map: meaning and properties
Map applies a given fucntion to each list element
`List(a1,a2,..,an).map(f)==List(f(a1),f(a2),...,f(an))`

Properties to keep in mind
- list.map(x => x) == list

- list.map(f.compose(g)) == list.map(g).map(f)

### Maps as function on lists
Sequential definitoin

	def mapSeq[A,B](lst: List[A], f: A => B): List[B] =lst match
		case Nil => Nil
		case h :: t => f(h) :: mapSeq(t, f)

we would like a version that parallelizes
- computations of f(h) for different elements h

- finding the elements themselves (list is not a good choice)

#### Question
Suppose we add parallel to this implementation in a straightforward way, replacing 
`f(h) :: mapSeq(t,f)` by the block:
`{ val (mh, mr) = parallel(f(h), mapSeq(t,f));   mh::mr }`

Now assume that f is a constant time operation (for example, + ). What is the asymptotic complexity on depth (time with unbounded parallelism) of invocation of such map on list of
length n ? Type just the expression that should go inside O(...) notation.

`O(n)`, We obtain recurrence equation, for `n > 0`

`D(n) = max(D(n-1), c1) + c2` Unfolding this a few times we get `D(n) = max( max(D(n-2), c1) + c2, c1) + c2`, `D(n) = max( max( max(D(n-3),c1) + c2  , c1) + c2, c1) + c2`

We see that in all but the possibly the innermost cases the first argument of max is larger, so max equals that first argument. So, we obtain, for example,
`D(n) = D(n-3) + c2 + c2 + c2`
Thus we conjecture `D(n) = n c2 + K`, which indeed satisfies the recurrence equation for suitably chosen constant K. Thus, `D(n)`  is in `O(n)`.

### Sequential map of an array producing an array

	def mapASegSeq[A, B](inp: Array[A], left: Int, right: Int, f: A=>B, out: Array[B]) =
		var i = left
		while (i < right)
			out(i) = f(inp(i))
			i = i+1

	val in = Array(2,3,4,5,6)
	val out = Array(0,0,0,0,0)
	val f = (x: Int) => x*x
	mapASegSeq(in, 1, 3, f, out)
	out

	res1: Array[int] = Array(0, 9, 16, 0, 0)

#### Question
How would a parallel version corresponding to this map on arrays look like?
Recall, for example, our segmentRec example for an unbounded number of threads.

	def mapASegSeq[A, B](inp: Array[A], left: Int, right: Int, f: A=>B, out: Array[B]) =
		if (right - left < threshold)
			mapASegSeq(inp, left, right, f, out)
		else
			val mid - left + (right - left)/2
			parallel(
				mapASegPar(inp, left, mid, f, out)
				mapASegPar(inp, mid, right, f, out)
			)

- writes need to be disjoint memory parts
- threshold needs to be large enough, otherwise lose efficiency

#### Example of using mapASegPar: pointwise exponent
Raise each array element to power p: `Array(a1,a2,...,an) -> Array(|a1|^p, |a2|^p,...,|an|^p)`
We can use previously defined higher-order functions:

	val p: Double = 1.5
	def f(x: Int): Double = power(x, p)
	mapASegSeq(inp, 0, inp.length, f, out)
	mapASegPar(inp, 0, inp.length, f, out)

Questions on performance
- are there performance gains from parallel execution
- performance of reusing higher order functions vs reimplementing

### Sequential pointwise exponent written from scratch

	def normOfPar(inp: Array[int], p: Double, left: Int, right: int, out: Array[Double]): Unit =
		if (right - left < threshold)
			var i = left
			while (i < right)
				out(i) = power(inp(i),p)
				i = i+1
		else
			val mid = left + (right - left)/2
			parallel(
				normsOfPar(inp, p, left, mid, out),
				normsOfPar(inp, p, mid, right, out)
			)

#### Question
How much performance improvement do you expect from:
- inlining the higher-order function of map
- parallelizing over several cores

*result*: very close performance of using parallelism vs. removal of higher order functions

#### Question
How does the implementation of map change if we work with trees instead of arrays?
from compilers perspective, trees are seen as arrays; hence simlar performance

### parallel map on immutable trees
Consider trees where
- leaves store array segments

- nonleaf node stores number of elements

	sealed abstract class Tree[A] {val size: int}
	case class Lef[A](a: Array[A]) extends Tree[A]
		override val size = a.size

	case calss Node[A](l: Tree[A], r: tree[A]) extends Tree[A]
		override val size = l.size + r.size

Assume trees are baalnced(can explore branches in parallel)

	def mapTreePar[A: Manifest, B.manifest](t: tree[A], f: A=> B): tree[B] =
	t match 
		case Leaf(a) =>
			val len = a.length; val b = new Array[B](len)
			var i = 0
			while (i < len) {b(i) = f(a(i)); i = i+1}
			Leaf(b)
		case node(l, r) =>
			val (lb, rb) = parallel(mapTreePar(l, f), mapTreePar(r, f))
			Node(lb, rb)

Speedup and performance is similar like arrays

#### Question 
What are the advantages of working with trees instead of arrays?

binary trees already have an order simiilar to tasks that should be executed in parallel; hence can travese to nodes and execute all tasks, while easily tracking depth

### Comparison of Arrays and immutable trees
*Arrays*:
- + random acces to elements on shared memory can share array

- + good memory locality

- - imperative: must ensure parallel tasks write to disjoint parts

- - expensive to concatenate

*Immutable trees*:
- + purely function, produce new trees, keep old ones

- + no need to worry about disjointness of write by parallel tasks

- + efficient to combine two trees

- - high memory allocation overhead

- - bad locality

## Lecture 2.3 - Parallel Fold (Reduce) Operation
### Fold: meaning and Properties
Fold takes among others a binary operation, but variants differ:
- whether they take an initial element or assume non-empty list

- in which order they combine operations of collection

	List(1,3,8).foldLeft(100)((s,x) => s-x) == ((100-1)-3)-8 == 88
	List(1,3,8).foldRight(100)((s,x) => s-x) == 1 - (3 - (8 - 100)) == -94
	List(1,3,8).reduceLeft((s,x) => s-x) == (1-3)-8 == -10
	List(1,3,8).reduceRight((s,x) => s-x) == 1-(3-8) == 6

To enable parallel operations, we look at *associative* operations
- addition, string concatenation but not minus

### Trees for Expression
Each expression built from values connected with (*) can be represented as a tree
- leaves are the values

- nodes are (*)

x (*) (y (*) z)

(x (*) y) (*) (z (*) w)

### Folding (reducing) trees
How do we compute the value of such an expression tree?

	sealed abstract class Tree[A]
	case class Leaf[A](value: A) extends Tree[A]
	case class Node[A](left: Tree[A], right: Tree[A]) extends Tree[A]

reults of evaluating the expression is given by a reduce of this tree. What is its sequential definition?

	def reduce[A](t: Tree[A], f: (A,A)=> A): A= t match
		case Leaf(v) => v
		case Node(l, r) => f(reduce[A](l, f), reduce[A](r, f))

We can think of `reduce` as replacing the constructor `Node` with given f

### Running Reduce
For non-associative operations, the result depends on structure of the tree

	def tree = Node(Leaf(1), Node(Leaf(3), Leaf(8)))
	def fMinues = (x: Int, y: Int) => x - y
	def res = reduce[Int](tree, fMinus)

#### Question
What is the simplest change we can make to the previous reduce of a tree that will give reasonable parallel code? Assume that the tree is balanced: left and right sub-trees are 
of the same size.

	def reduce[A](t: Tree[A], f: (A,A)=> A): A= t match
		case Leaf(v) => v
		case Node(l, r) => 
			val (lV, rV) = parallel(reduce[A](l, f), reduce[A](r, f))
			f(lV, rV)

#### Question
If the tree t has height n, what is the computation depth of reduce on such tree?

n, the computation tree has the size and shape of the tree t passed as an argument, so the depth of the computation tree is the height of the tree t.

### Associativity stated as tree reduction
how can we restate associatvity of such trees?

	reduce(Node(Leaf(x), Node(Leaf(y), Leaf(z))), f) == reduce(Node(Node(Leaf(x), Node(Leaf(y)), Leaf(z)), f)

### Order of elements in a tree
Observe: can use a list to describe the ordering of elements of a tree

	def toList[A](t: Tree[A]): List[A] = t match
		case Leaf(v) => List(v)
		case Node(l, r) => to List[A](l) ++ toList[A](r)

Suppose we also have tree map:

	def map[A,B](t: Tree[A], f: A => B): tree[B] = t match
		case Leaf(v) => List(f(v))
		case Node(l, r) => Node(map[A, B](l, f), map[A, B](r, f))

Can you express `toList` using `map` and `reduce`?

#### Question
Think of a Scala expression that uses the map and reduce on trees to compute `toList(t)`

	toList(t) == reduce(map(t, List(_)), _ ++ _)

### Consequence state as tree reduction
Consequence of associativity: consider two expressions with same list of operands connected with (*), but different parentheses. Then these expressions evaluate to the same result

Express this consequence in Scala using fucntions we have defined so far. 

#### Question
How would you use functions we have defined so far to state the mentioned theorem that, for associative operations, the nesting of parentheses does not matter?

Consequence if f: (A, a) => A is associative, t1: Tree[A] and t2: Tree[A] and if toList(t1)==toList(t2), then: reduce(t1, f)==reduce(t2,f)

### Towrads a reduction for arrays
We have seen reduction on trees. often we work with colelctions where we only know the ordering not the tree structure. How can we do reduction in case of, e.g., arrays?
- convert it into a balanced tree

- do tree reductions

Because of associativity we can choose any tree that preserves the order of eleemnts of the orignial colelction. tree reducers replace Node constructors with f,m so we can jhust use 
f, directly instyead of buildign treee nodes. When the segment is small, it is faster to process it sequentially.

	def ReduceSeg[A](xs.Array[A], left: Int, right: int, f: (A, A) => A): A =
		if (right - left < threshold)
			var res = inp(left); var i = left +1
			while (i < right) {res = f(res, inp(i)); i = i+1}
			res
		else
			val mid = left + (right - left)/2
			val (a1, a2) = parallel(reduceSeg(inp, left, mid, f), reduceSeg(inp, mid, right, f))
			f(a1, a2)

	def reduce[A](inp: Array[A], f: (A,A) => A): A =
		reduceSeg(inp, o, inp.length, f)

## Lecture 2.4 - Associativity I
### Associative Operation
Operation f: (A,A) => A is associative iff for every x, y, z
`f(x,f(y,z)) = f(f(x,y),z)

Consequence:

- two expressions with same list of operands connected with (*), but different parentheses evaluated to the same result

- reduce on any tree with this list of operands gives the same result

Which operations are associative?

### A different property: commutativity
Operation f: (A,A) => A is **Commutative** iff for every x, y: f(x,y) = f(y,x)

There are operations that are associative but not commutative. There are operations that are commutative but not associative. For correctness of `reduce`, we need (just) associativity.

#### Example of oeprations that are both assocaitive and commutative
Many operations from math:
- addition and multiplication of mathematical integers (BigInt) and of exact rational numbers (given as, e.g., pairs of `BigInts`)

- addition and multiplication modulo a positive integer (e.g. 2^32), including the usual arithmetic on 32-bit Int or 64-bit Long values

- union, intersection, and symmetric difference of sets

- union of bags (multisets) that preserves duplicate elements

- boolean operations &&, ||, exclusive or

- addition and multiplication of polynomials

- addition of vectors

- addition of matricies of fixed dimension

### Using Sum: Array norm
our array norm example computes first: `sum(s, t-1)roundDown(|ai|^p)` Which combinations of operations does sum of powers correspond to?

#### Question 
Which combination of map and reduce gives the sum of powers of all array elements for s=0 and t= a.length?

	reduce(map(a, power(abd(_), p)), _ + _)

Here + is the associative operation of reduce. map can be combined with reduce to avoid intermediate colelctions.

### Making an operation commutative is easy
Suppose we have a binary operation g and a strict total ordering less

Then this operation is commutative:

	def f(x: A, y: A) = if (less(y, x), g(y, x) else g(x, y)

Indeed f(x,y)==f(y,x) because:
- if x==y then both sides equal g(x,x)

- if less(y, x) then left sides is g(y,x) and its not less(x, y) so right side is also g(y,x)

- if less(x, y) then its not less(y, x) so left sides is g(x, y) and right side is also g(x, y)

We know of no such efficient trick for associativity

## Lecture 2.5 - Associativity II
### Associative operations on tuples
Suppose f1: (A1,A1) => A1 and f2: (A2,A2) => A2 are associative. Then ((A1,A2), (A1,A2)) => (A1,A2) defined by

f((x1,x2),(y1,y2)) = (f1(x1,y1),f2(x2,y2))

is also associative:

	f(f((x1,x2),(y1,y2)),(z1,z2)) ==
	f((f1(x1,x2),f2(y1,y2)),(z1,z2)) ==
	(f1(f1(x1,y1),z1), f2(f2(x2,y2),z2)) ==
	(f1(x1,f1(y1,z1)), f2(x2,f2(y2,z2))) ==
	f((x1, x2), (f1(y1,z1), f2(y2,z2))) ==
	f((x1, x2), f((y1,y2), (z1,z2)))

We can similarly construct associative operations on for n-tuples

#### Example: Rational Multiplication
Suppose we use 32-bit numbers to represent numerator and denominator as a rational. We can define multiplication working on pairs of numerator and denominator

	times((x1,y1),(x2,y2)) = (x1*x2,y1*y2)

#### Example average
Given a collection of integers, compute the average

	val sum = reduce(collection, _+_)
	val length = reduce(map(collection, (x: Int) => 1), _+_)
	sum/length

Solution with a single call to reduce

	val (sum, length) = reduce(map(collection, (x: Int) => (x,1), _+_)
	val average = sum / length

### Associativity through symmetry and commutativity
Although commutativity iof f alone does not imply associativity, it implies it if we have additional property. Define:

	E(x,y,z) = f(f(x,y),z)

Say arguments of E can rotate if E(x,y,z) = E(y,z,x) that is:

	f(f(x,y),z) = f(f(y,z), x)

Claim: if f is commutative adn arguents of E can rotate then f is also associative. 

Proof: `f(f(x,y),z) = f()f(y,z), x) = f(x, f(y, z))`

#### Example: addition of modular fractions
Define

	plus((x1,y1),(x2,y2)) = (x1*y1 + x2*y1, y1*y2)

Such plus associative? yes

observe commutative, can prove through property that associative. 

#### Example: Relativistic Velocity Addtition
Let u, v range over rational numbers in th eopen interval (-1,1). Define f to add velicities according to special relativity `f(u,v) = (u+v)/(1+uv)`. Clearly, f is commutative:
Can also rotate arguments. 

I implement expression using floating points, note- floating points are not associative. So results of reduceLeft may differ substantially.(Rouding)

### A family of associative operations
DEFINE BINARY OPERATIONS ON SETS `A,B` BY `F(A,B) = (AORB)*` WHERE * IS ANY OPERATOR ON SETS (CLOSURE) WITH THESE PROPERTIES:
- A SUB A* (expansion)
- A sub B then A* sub B (monotonicity)
- A** = A* (idempotence)

Example of *: convex hull, Kleene star in regular expressions. 

Claim: such f is associative

Proof: f is commutative. It remains to show

f(f(A,B),C) = ((AorB)*orC)* = (AorBorC)*

because there it is easy that the arguments rotate.
- Since Aor B monotonicity, then sub of AorBorC

- So C sub AorBorC sub (AorBorC)*

- Thus by monoticity and idempotence ((AorB)*orC)* sub ((AorBorC)*)* = (AorBorC)*

Now prove other way for =
- AorB sub(AorB)*

- Thus by monoticity (AorBorC)* sub (AorB)*orC

## Lecture 2.6 - Parallel Scan (Prefix Sum) Operation
### Parallel Scan
now examine parallel **scanLeft**
- list of the folds of all list prefixes

	List(1,3,8).Scanleft(100)((S,X) => S+X) == List(100,101,104,112)

	List(a1,a2,a3.Scanleft(a0)(f) == List(b0,b1,b2,b3)

Assume f is associative, scanRight is different from scanLeft, even if f is associative. We consider only scanLeft, but scanRight is dual

	List(1,3,8).scanRight(100)((S,X) => S+X) == List(112,111,108,100)

### Sequential Scan Solution

	def scanLet[A](inp: Aray[A], a0: A, f: (A,A) => A, out: Array[A]): Unit =
		out(0) = a0
		var a = a0
		var i = 0
		while (i < inp.length)
			a = f(a, inp(i))
			i = i+1
			out(i) = a

### Can make scanLeft parallel
Assuming f is associative. Goal: Algo runs in O(logn) time given infinite parallelism

	def scanLeft[A](inp: Array[A], a0: A, f: (A,A) => A, out: Array[A]): Unit =
		def reduceSeg[A](inp: Array[A], left: Int, 
			right: Int, a0: A, f: (AA,A) => A): A =
			...
		def mapSeg[A,B](inp: Array[A], left: Int, 
			right: Int, fi: (Int, A) => B, out: Array[B]): Unit =
			...
		
		val fi = {(i: Int, v:A) => reduceSeg(inp, 0 , i, a0, f)}
		mapSeg(inp, 0, inp.length, fi, out)
		val last = inp.length -1
		out(last + 1) = f(out(last), inp(last))
		
#### Reusing intermediate results
Previous solution, dont resuse any copmutations. Can we reuse some. reduce proceeds by applying the operations in a tree

Idea: Save the intermediate reults of this parallel computation. We first assume that input collection is also another tree

### Tree Definitions
Trees storing our input collection only have values in leaves:

	sealed abstract class Tree[A]
	case class Leaf[A](a: A): extends Tree[A]
	case class Node[A](l: Tree[A],r: Tree[A]): extends Tree[A]

Trees storing intermediate values also have `res` values in nodes

	sealed abstract class TreeRes[A] {val res: A}
	case class LeafRes[A](override val res: A): extends TreeRes[A]
	case class NodeRes[A](override val res: A,l: Tree[A],r: Tree[A]): extends TreeRes[A]
	
	def upsweep[A](t: Tree[A], f: (A,A) => A): TreeRes[A] = t match
		case Leaf(v) => leafRes(v)
		case Node(l,r) =>
			val (tL,tR) = parallel(upsweep(l,f), upsweep(r, f))
			NodeRes(tL, f(tL.res, tR.res), tR)
	def downsweep[A](t: Tree[A], a0: A, f: (A,A) => A): TreeRes[A] = t match
		case Leaf(a) => leafRes(f(a0,a))
		case Node(l, _, r) =>
			val (tL,tR) = parallel(downsweep[A](l,a0,f), 
						downsweep[A](r, f(a0,l.res),f))
			Node(tL, tR)

	def scanLeft[A](t: Tree[A], a0: A, f: (A,A) => A): Tree[A] =
		def prepend[A](x: A, t: Tree[A]): Tree[A] = t match
			case Leaf(v) => Node(Leaf(x), Leaf(v))
			case Node(l, r) => Node(prepend(x, l), r)
			
		val tRes = upseep(t, f)
		val scan1 = downsweep(tRes, a0, f)
		prepend(a0, scan1)

### Intermediate treee for array reduce

	sealed abstract class TreeResA[A] {val res: A}
	case class Leaf[A](from: Int, to: Int, override val res: A) extends TreeresA[A]
	case class Node[A](l: TreeresA[A], override val res: A, r: TreeResA[A]) extends TreeResA[A]

The only difference compared to previous TreeRes: each Leaf now keeps track of the array segment range (from, to) from which `res` is computed. We do not keep track of the array 
elements in the Leaf itself; we instead ass around reference to the input array.

#### Upsweep on Array
Starts from an array, produces a tree

	def upsweep[A](inp: Array[A], from: int, to: Int, f: (A,A)=> A): TreeResA[A] =
		if (to - from < threshold)
			Leaf(from, to, reduceSeg1(inp, from +1, to, inp(from), f))
		else
			val mid = from + (to- from)/2
			val (tL,tR) = parallel(
				upsweep(inp, from, mid, f)
				upsweep(inp, mid, to, f)
			)
			Node(tL, f(tL.res, tR.res), tR)

#### Sequential reduce for segment

	def reduceSeg[A](inp, Array[A], left: Int, right: Int, a0: A, f: (A,A) => A): A =
		val a = a0
		var i = left
		while (i < right)
			a = f(a, inp(i))
			i = i+1
		a

#### Sequential scan left on Segment

	def scanLeftSeg[A](inp: Array[A], left: int, right: int, a0: A, f:(A,A) => A, out: Array[A]) =
		if (left , right)
			val i = left
			var a = a0
			while (i < right)
				a = f(a, inp(i))
				i = i+1
				out(i) = a

#### Downsweep on Array

	def downsweep[A](inp: Array[A], t: TreeResA[A], a0: A, 
			f: (A,A) => A, out: Array[A]): Unit = t match
		case Leaf(a) => scanLeftSeg(inp, from, to, a0, f, out)
		case Node(l,_,r) =>
			val (_,_) = parallel(
				downsweep[A](inp,a0,f,l,out), 
				downsweep[A](inp,f(a0,l.res),f,r,out)
			)

### Finally: Parallel scan on the array

	def scanLeft[A](np: Arry[A], a0: A, f: (A,A) => A, out: Array[A]) =
		val t = upsweep(inp, 0, inp.length, f)
		downsweep(inp, a0, f, t, out)
		out(0) = a0
