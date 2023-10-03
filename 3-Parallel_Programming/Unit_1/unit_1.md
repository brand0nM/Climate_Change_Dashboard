# Parallel Programming 
## Lecture 1.1 - Introduction to Parallel Computing
### What is Parallel Computing
*parallel computing* is a type of computation in which many calculations are performed at the same time.
Basic Prinicple: copmutations can be divided into smaller subproblems each of which can be solved simultaneously.
Assumption: we have parallel hardware at our disposal which is cap[able executing these computations in parallel.

### History
Parallel computing was present since early days of computing; 1st example was the analytical engine. 
Idea was that dependednt computations occur at the same time. In the 20th centruy IBM built some fo the first commercial parallel
computers; motivation was the orginization of a single computer has reached its limits, so one can make a more advanced
machine by interconnecting muplitple computers. At time was confined to specific compunities useing high performance computers;
i.e. crypography, physics, math, etc. 

#### Recent History
At the begining of 21st century processor frequency clasing hit a *power wall*; physics princple due to non linear growth 
proportional to frequency. Processor vendors decideed to provide muiltiple cores on same processing chip.
Common theme: parallel computing provides computational power when sequential computing cannot do so.

### Why Parallel Computing
much harder than sequential programming
- separating sequential computations into parallel subcomputations can be challenging or even impossible

- ensuring program correctness is more difficult, due to new types of erroes

*speedup* is the onlt reason why we bother paying for this complexity

### Parallel Proramming vs Concurrent Programming
Parallelism and concurrnecy are closely related concpts

- Parallel program: usses parallel hardware to execute computations more quikly. efficiency is its main concern.
i.e. division into subproblems, optimal use of parallel hardware. Speed up

- Concurrent program: may or may not execute multiple executions at the same time. Improves modulatiry responsiveness or 
maintainability. I.e. when can an execution start, how can information exchange occur, how to manage access to shared resources.
Convenience

May overlap, but distinct 

### Parallelism Granularity
Parallelism manifest itself at different granularity levels
- bit lelvel parallelism: processing multiple bits of data in parallel

- instruction level parallelism: executing different instructions from the same instruction stream in parallel

- task-level parallelism: executing separate instruction streams in parallel

course mainly focuses on task-level

### Classes of Parallel computers
many different forms of parallel hardwRE
- MULTIPLE-CORE PROCESSORS

- SYMMETRIC MULTIPROCESSORS

- GRAPHICS PROCESSING UNIT

- FIELD-PROGRAMMABLE GATE ARRAYS

- COMPUTER CLUSTERS

!()["pictures/visual_hardware.png"]
Will focus on multicore and SMP. algo's can be generalized and implemented on most kinds of hardware

## Lecture 1.2 - Parallelism on JVM
many forms of parallelism. Assume we are running mulkticore or processor with shared memory. Operating system and the JVM as
underlying runtime environments

### Processes
Operating system - software that manages hardware and software resources, and schedules program execution.

Process- an instance of a program that is executing in the OS
the same program can be started as a rocess more than once, or even simultaneously in the same OS.
I.e. each instance of SBT opened will have a unique PID indetifier.

operating system multiplexes many differnt processes and a limited number of CPU's, so that they get *time slices* of execution. 
Called *multitasking*. Two different processes cannot access eachothers memory directly, they are isolated

### Threads 
Each process can contain multiple indepent concurrency units called threads. Threads can be started from within the same program, 
and they share the same memory address space. I.e one thread can write while the other reads data. Each thread has a program 
counter and a program stack. *program set* contains a sequence of the programs being executed. *program counter* is the position
in its current method. each program has its own stack ian is only acccesible within that specific program. To communicate JVM
processes must write to memories.

### Creating and starting threads
Each JVM process starts with a *main thread*
To start additional threads

1) define a new Threads subclass

2) Instantiate a new Thread object

3) Call start on the Threads object

The threads subclass defines the code that the thread will execute. The same custome Threads cubclass can be used to start 
multiple threads


#### Example 1.2.1

	class HelloThread extends Thread
		override def run()
			println("Hello World")

	val t = new HelloThread
	t.start() // runs in parallel to the main thread, then
	t.join() // join runs- only after the second thread has completed- on the main thread

#### Example 1.2.2

	class HelloThread extends Thread
		override def run()
			println("Hello World")

	def main()
		val s = new HelloThread
		val t = new HelloThread
		s.start() // runs in parallel to the main thread, then
		t.start()
		s.join() // join runs- only after the second thread has completed- on the main thread
		t.start()
		t.join()

In some instances it will print hello World twice, but others it will print Hello Hello ... The issue is that seperaate statements
in different threads can overlap. To ensure a sequence of statements in a specifric thread execute at once, must implement an 
*atomic* if it appears as if it occurs instantaneously from thew POV of other threads.

	private var uidCount = 0L
	def getUniqueId(): Long =
		uidCount = uiodCount +1
		uidCount

	def startThread() =
		val t = new Thread
			override def run()
				val uids = for (i <- 0 unitil 10) yield getUniqueId()
				println(uids)
		t.start()
		t

Exapmle shows that getUinqueID method does not occur atomically; staements like this won't necessarily work on seperate cores,
processors

!()["pictures/atomicity.png"]

### Synchronized Block
The synchronized block is used to achieve atomoicity. code clock after synchronized call on an object x is necer executed by two
threads at the same time.

	private val x - new AnyRef{}
	private var uidCount = 0L
	def getUniqueId(): Long = x.synchronized
		uidCount = uidCount +1
		uidCount

## Lecture 1.3 - Parallelism on the JVM II
### Composition with the synchornized block
Invocations of synchronized blocks can be nested

	class Account(private var amount: Int = 0)
		def transfer(target: Account, n: Int) =
			this.synchronized
				target.synchronized
					this.amount -= n
					target.amount += n
	def startThread(a: Acocunt, b: Acount, b: Int)
		val t = new Thread
			override def run()
				for (i <- 0 until n)
					a.transfer(b, 1)
		t.start()
		t

	val t = startThread(a1,a2,150000)
	val s = startThread(a2,a1,150000)
	t.join
	s.join

### Deadlocks
Deadlock happens when two or more threads compete for resources and wait for each to finish without releasing those aquired.

	val a = new Account(50)
	val b = new Account(70)
	
	a.transfer(b,10)
	b.transfer(a,10)


### Resolving Deadlock
One approach is to always aquire resources in the same order. This assumes an ordsering relationship on the resoures
best way to do this iss by giving unique id's and ensuring porocerssing occur in some given order.

	val uid = getUniqueUid()
	private def lockAndTranfer(target: Account, n: int) =
		this.synchronized
			this.amount -= n
			target.amount += n
	def transfer(target: Account, n: Int) =
		if (this.uid < target.uid) this.lockAndTransfer(target, n)
		else target.lcokAndTransfer(this, -n)

### Memory Model
A set of rules that describe how threads interact ewhen accessing shared memory. Java Memory Model

1) Two threads writing to seperate locations in memory do not need synchronization

2) A thread X calls join on another thread Y is guaranteed to observe all the writes by thread Y after join returns

## Lecture 1.4 - Running Computations in Parallel
### Basic Parallel construction
Given expressions `e1` and `e2` compute them in parallel and return the pair of results `parallel(e1,e2)`

###  Example: computing p-norm
Given a vector as an array (of integers), compute its p-norm. A p-norm is a generalization of the notion of length from geometry.
2-norm of a two-dimensional vector `(a1,a2)` is `(a1^2+a2^2)^(1/2)`. p-norm of a vector `(a1,..,an)` is `(Sum(1,..,n)|ai|^p)^(1/p)`.

### Main Step: sum of powers of array segment
First, solve *sequentially* the following `sumSegment` problem: given
- an integer array a , representing our vector

- a positive double floating point number p

- two valid indices s <= t into the array a

compute `Sum(s,...,t-1)roundDown(|a1|^p), rounds down to an integer

	def sumSegment(a: Array[Int], p: Double, s: Int, t: Int): Int =
		var i = s; var sum: Int = 0
		while (i<t)
			sum = sum + power(a(i),p)
			i = i+1
		sum
	
	def power(x: Int, p: Double): Int = math.exp(p * math.log(abs(x))).toInt

### P norm implementation

	def sumSegment(a: Array[Int], p: Double, s: Int, t: Int): Int =
		var i = s; var sum: Int = 0
		while (i < t)
			sum = sum + power(a(i), p)
			i = i + 1
	
	def power(x: Int, p: Double): Int = math.exp(p * math.log(abs(x))).toInt
	def pNorm(a: Array[Int], p: Double): Int = power(sumSegment(a, p, 0, a.length), 1/p)

How can we distribute workloads on a seperate hardware? Try splitting half the sum at some element m; so take 1-m and m-(length-1).

	def pNormTwoPart(a: Array[Int], p: Double): Int = 
		val m = a.length/2
		val (sum1, sum2) = (sumSegment(a, p, 0, m),
			sumSegment(a, p, m, a.length))

		power(sum1 + sum2, 1/p)

Now to make use of parallel threads.

	def pNormTwoPart(a: Array[Int], p: Double): Int = 
		val m = a.length/2
		val (sum1, sum2) = parallel(sumSegment(a, p, 0, m),
			sumSegment(a, p, m, a.length))

		power(sum1 + sum2, 1/p)
	
!()["pictures/parallel_comp.png"]

### Recursive Algo for an unbound number of threads

	def segmentRec(a: Array[Int], p: Double, s: Int, t: int) =
		if (t - s < threshold)
			sumSegment(a, p, s, t)
		else
			val m = s + (t - s)/2
			val (sum1, sum2) = parallel(segmentRec(a, p, s, m),
						segmentRec(a, p, s, m))
			sum1 + sum2

### Signature of Parallel

	def parallel[A, B](taskA: => A, taskB: => B): (A,B) = {...}

- returns the same value as given

- benefit: parallel(a, b) can be faster than (a, b)

- it takes its arguments as by name

Whats the difference in taking parameters as call by name and value?
What is the difference between the two signature and, correspondingly, the two invocations?

Because the parameters of parallel1 are call by value, a and b are evaluated sequentially in the second case, 
not in parallel as in the first case.

Benefits of parallelism, need unevaluated parameters

#### What happens inside a system when we use parallel
Efficient parallelism requires support from

- language and libraries

- virtual machine

- operating system

- hardware

One implementation of `parallel` uses JVM threads

- those typically map to operating system threads

- operating system can schedule different threads on multiple cores

Given sufficient resources, a parallel program can run faster

### Underlying Hardware Architecture Affects Performance
Consider code that sums up array elements instead of their powers:

	def sum1(a: Array[Int], p: Double, s: int, t: Int): Int =
		var i = s; var sum: Int = 0
		while (i <t)
			sum = sum + a(i)
			i = i +1
	sum
	val ((sum1, sum2),(sum3,sum4)) = parallel(
	parallel(sum1(a, p, 0, m1), sum1(a, p, m1, m2)),
	parallel(sum1(a, p, m2, m3), sum1(a, p, m3, a.length)))

Why is it difficult to obtain speedup for sum1 invocations even though this was possible when we did a more complex operation
on array elements? Memory is the bottleneck; hence must consider not only the number of cores, but also the memory constraints

## Lecture 1.5 - Monte Carlo Method to Estimate Pi
### A method to Estimate Pi (3.14..)
Consider a squarte and a circle of radius one inside a square. Then the ratio between surfaces of 1/4 of a circle and 1/4 of a
square: lambda = Pi/4. Randomly sample points inside the square, count how many fall inside the circle; then multiply ratio by 4 
for an estimate of Pi.

### Sequential Code for Sampling Pi

	import scala.util.Random
	def mcCount(iter: Int): Int =
		val randomX = new Random
		val randomY = new Random
		var hits = 0
		for (i <- 0 until iter)
			val x = randomX.nextDouble
			val y = randomY.nextDouble
			if (x*x + y*y < 1) hits = hits + 1
		hits
	def monteCarloPiSeq(iter: Int): Double = 4.0 * mcCount(iter)/iter

### Four way parallel code Sampling Pi

	def monteCarloPiPar(iter: int0: Double =
		val ((pi1,pi2),(pi3,pi4)) = parallel(
			parallel(mcCount(iter/4),mcCount(iter/4)),
			parallel(mcCount(iter/4),mcCount(iter - 3*(iter/4))))
			4.0 * (pi1 + pi2 + pi3 + pi4) / iter

## Lecture 1.6 - First-Class Tasks
### More Flexible Construct for Parallel Computation

	val (v1, v2) = parallel(e1, e2)

we can write alternatively using the task construct

	val t1 = task(e1)
	val t2 = task(e2)
	val v1 = t1.join
	val v2 = t2.join

t = task(e) starts computation e "in the background"
- t is a task, which performs computations of e

- current computations proceed in parallel with t

- to obtain the result of e, use t.join

- t.join bloocks and waits until the result is computed

- susequent t.join calls quickly return the same result

### Task Interface
here is a minimal interfacew fdor tasks

	def task(c: => A) : Task[A]
	trait Task[A]
		def join: A

`task` and `join` establish maps between computations and tasks in terms of the value computed the equation `task(e).join==e` holds
We can omit writing `.join` if we also define an implicit conversion:

	implicit def getJoin[T](x: Task[T]): T = x.join

#### Example: Starting four tasks

	val ((part1, part2),(part3, part4)) =
		parallel(parallel(sumSegment(a,p,0,mid1),
				sumSegment(a,p,mid1,mid2)),
			parallel(sumSegment(a,p,mid2,mid3),
				sumSegment(a,p,mid3,a.length)))
	power(part1 + part2 + part3 + part4, 1/p)

With task implementation

	val t1 = task{sumSegment(a,p,0,mid1)}
	val t2 = task{sumSegment(a,p,mid1,mid2)}
	val t3 = task{sumSegment(a,p,mid2,mid3)}
	val t4 = task{sumSegment(a,p,mid3,a.length)}
	power(t1 + t2 + t3 + t4, 1/p)

### Can we define parallel using task?
Suppose you are allowed to use task. Implement parallel construct as a method using task

Right

	def parallel[A,B](cA: => A, cB: => B): (A, B) = 
		val tA = cA
		val tB = task{cB}
		
		(tA, tB.join)

Wrong

	def parallel[A,B](cA: => A, cB: => B): (A, B) = 
		val tA = cA
		val tB = (task{cB}).join
		
		(tA, tB)

Does not compute the tasks in parallel, first computes tA then tB

## Lectur 1.7 - How fast are Parallel Programs?
### How long does out computation take?
performance: a key motivation for `parallelism`
How to estimate it?
- empirical measurement

- asymptotic analysis

Asymptotic analysis is important to understand how algorithms scal when:
- input get larger

- we have more hardware parallelism available

We examine worst-case (as opposed to average) bounds

### Asymptotic Analysis of Sequential Running Time
Learned how to chracterize behaviors of *sequential* programs using the number of operations they perform as a funciton of arguments
- inserting an integer into a sorted linear list takes O(n), for list sorting n integers

- inserting into an integer into a balanced binary tree of n integers takes time O(logn), for tree storing n integers

Lets review these techniques by applying them to `sumSegment`

Find time boundon sequential `sumSegment` as a function of s and t

	def sumSegment(a: Array[Int], p: Double, s: Int, t: int): int =
		var i = s; var sum: Int = 0
		while (i < t)
			sum = sum + power(a(i), p)
			i = i + 1
		sum

We are looking for a precise upper bound on the complexity in the form O(f) where f involves s and t. What is the function f? 
O(t-s)

The answer is W(s,t) = O(t -s) a function of the form: c1(t - s) + c2
- t - s loop iterations
- a constant amount of work in each iteration

### Analysis of recursive functions

	def segmentRec(a: Array[Int], p: Double, s: Int, t: Int) =
		if (t - s < threshold)
			sumSegment(a, p, s, t)
		else
			val m = s + (t - s)/2
			val (sum1, sum2) =(segmentRec(a, p, s, m),
					segmentRec(a, p, m, t))
			sum1 + sum2

Analogously to sumSegment, think about an upper bound on the asymptotic running time of segmentRec.
!()["pictures/tree_computations.png"]

W(s,t) =
- c1(t-s)+ c2, if t - s < threshold
- W(s, m) + W(m, t) + c3, otherwise for m = roundDown((s+t)/2)

#### Bounding Solution of recurrence equation
W(s,t) =
- c1(t-s)+ c2, if t - s < threshold
- W(s, m) + W(m, t) + c3, otherwise for m = roundDown((s+t)/2)

Assume t-s = 2^N(threshold - 1), where N is the depth o fthe tree. Computation tree has 2^N leaves and 2^N -1 internal nodes.
W(s,t) = 2^N(c1(threshold -1)+c2)+(2^N -1)c3 = 2^Nc4 + c5

Thus if 2^(N-1) < (t - s)/(threshold -1) <= 2^N, we have

W(s, t) <= 2^N*c4 + c5 < (t - s)*2/(threshold -1) + c5

W(s, t) is in O(t - s). Sequential `segmentRec` is linear in t - s

Hence work is linear in t - s.

### Solving recurrence with unbound parallelism

	def segmentRec(a: Array[Int], p: Double, s: Int, t: Int) =
		if (t - s < threshold)
			sumSegment(a, p, s, t)
		else
			val m = s + (t - s)/2
			val (sum1, sum2) = parallel(segmentRec(a, p, s, m),
					segmentRec(a, p, m, t))
			sum1 + sum2

D(s,t) =
- c1(t-s)+ c2, if t - s < threshold
- max(D(s, m) + D(m, t)) + c3, otherwise for m = roundDown((s+t)/2)

What is the solution of the recurrence equation for D(s,t)? Do you expect it to be a faster or slower growing function and
by how much? 

Assume t -s = 26N(threshold -1), where N is the depth of the tree. Computation tree has 2^N leaves and 2^N -1 internal nodes.
The value of D(s,t) in leaves of computation tree: c1(threshold - 1) + c2. One level above: c1(threshold -1) + c2 + c3

Root: c1(threshold -1) + c2 + (N-1)c3. Hence solution bounded by O(N) and running time is monotonic in t-s, or
If 2^N-1 < (t -s)/(threshold -1) <= 2^N, we hgave N < log(t -s) + c6. and D(s,t) will run in O(log(t-s)).

### Work and Depth
Want to consider the complexity of parallel code
- but this depends on available parallel resources

- we introduce two measures for a program

Work W(e): number of steps e would take if there was no parallelism
- this is ismply the sequential execution time

- treat all parallel(e1, e2) as (e1, e2)

Depth D(e): number of steps if we had unbounded parallelism
- we take maximum of running times for arguments of parallel

### Rules for Depth (span) and work
key rules are
- W(parallel(e1, e2)) = W(e1) + W(e2) + c2

- D(parallel(e1,e2)) = max(D(e2),D(e2)) + c1

if we divide work in equal parts, for depth it counts only onces. For parts of code where we do not use parallel explicitly we must
add up costs. For function call or operation f(e1, ..., en):
- W(f(e1,..., en)) = W(e1) + ... + W(en) + W(f)(v1,.., vn)

- D(f(e1,..., en)) = D(e1) + ... + D(en) + D(f)(v1,.., vn)

Here vi denotes values of ei. If f is primitive operation on integers, then W9f) and D9f) are constant functions, regardless of vi.
Note: we assume (reasonably) that constants are such that D <= W

What does the definition `W(f(e1,...,en)) = W(e1) + ... + W(en) + W(f) (v1,...,vn)` assume about the signature of f?
The arguments of f are call by value; Indeed, we are adding up W(e1),...,W(en) because we assume that these arguments are evaluated.
Rules for call by name arguments, as well as rules for task become substantially more complex.

### Computing time bound for given parallelism
Suppose we know W(e) and D(e) and our platform has P parallel threads. Regardless of P, cannot finish sooner than D(e) because of
dependencies. Regardless of D(e), cannot finish sooner than W(e)/P: every piece of work needs tto be done. So its reasonable to
use this estimate for running time: `D(e) + W(e) /P`. Given W and D, we can estimate how programs behave for different P
- If P is constant but input grow, parallel programs have same asymptotic time complexity as sequential ones

- even if we have infinite resources, (P -> infinity), we have non-zero complexity given by D(e)

### Consequences for segmentRec
The call to parallel function `segmentRec` had:
- work W: O(t- s)

- depth D: O(log(t - s)

on a platform with P parallel threads the runnning time is, for some constants b1,b2,b3,b4:
b1log(t-s) + b2 + (b3(t-s) + b4)/P

- if P is bounded, we have linear behavior in t -s
 - possibly faters than sequential, depending on constants
- if P grows, the depth starts to dominate the cost and the running time becomes logarithmic

### Parallelism and Amdahl's Law
Suppose that we have two parts of sequential computation:
- part1 takes fraction f of the computation time (e.g. 40%)

- part 2 take the remaining 1-f fraction of time (e.g.60%) and we can speed it up

If we make part2 P times faster the speedup is 1/(f+(1-f)/P)

for P = 100 and f=.4 we obtain 2.46. Even if we speed the second part infinitely we can obtain at most 1/.4= 2.5 speedup

What is the limit of the function 1/(f+(1-f)/P) when f is constant and P goes to infinity? 1/f, highlights the fact taht some parts 
of the computation can not be sped up.

## Lecture 1.8 - Benchmarking Parallel Programs
### testing and Benchmarking
- testing: ensures that parts of the program are behaving according to the intended behavior

	val xs = List(1, 2, 3)
	assert(xs.reverse == List(3, 2, 1))

- benchmarking: computes performance metrics for parts of the program; could be runnign time, memory footprint, metric traffic,
disk usage, or latency.

	val xs = List(1, 2, 3)
	val startTime = System.nanoTime
	xs.reverse
	println((System.nanoTime - startTime)/1000000)

nieve way to calc runtime, but statrs to illustrate

Typically, testunbg yields a binary output: a program or its paprt is either correct or its not.

Benchmarking usually yields a continuous value, which denotes the extent to which the program is correct.

### Benchmarking Parallel Programs
Why do we benchmark parallel programs?

Performance benefits are the main reason why we are writing parallel programs in the first place. Benchmarking parallel programs is
even more important than benchmarking sequential programs.

#### Performance Factors
Performance (specifically, running time) is subject to many factors:
- processor speed

- number of processors

- memory acces latency and throughput (affects contention)

- cache bahavior (e.g. false sharing, associativity effects)

- runtime behavior (e.g. garbage collection, JIT compilation, thread scheduling)

!()["pictures/visual_usage.png"]

Further Reading: "What every programmer should know about memory" by Ulrich Drepper

### Measurement Methodlogies
measuring performance is difficult- usually, the a performance metric is a random variable.
- multiple repetitions

- statistical treatment- computing mean and variance

- eliminating outliers

- ensuring steady state (warm up)

- preventing anomalies (GC (avoid, allocating specific memory before, JIT compilation (disabled for purposes of running benchmark),
aggressive optimizations(manual code restucture because benchmark is too simple))

	val xs = List(1, 2, 3)
	val startTime = System.nanoTime
	xs.reverse
	println((System.nanoTime - startTime)/1000000)

did not run multiple times -> eliminate outliers or anomalies or ensured its in a steady state beore benchmarking, another problem
is the list is so short, output will be very noisy

To learn more, see Statistically Rigorous Java Performance Evalutation, by Georges, Buytaert, Eeckhout

!()["pictures/Visual_benchmarks.png"]

### ScalaMeter
a benchmarking and performance regression testing framework for the JVM
- performance regression testing: comparing performance of current program run against known previous runs

- benchmarking: measuring performance of the current (part of the) program

we will focus on benchmarking

 First add as a dependency

	libraryDependencies +=
		"com.storem-enroute" %% "scalameter-core" % "0.6"

Then import scalaMeter package

	import org.scalameter._

	val time = measure
		(0 until 1000000).toArray
	println(s"Array initialization time: $time ms")

Will have a big variance in output values. 

1) JVM started a dynamic compliation; JVM optimized something and subsequent mean decreased

2) Another thing is GC cycle started; memory is deallocated and return the memory subsystem only after the memory occupancy raises
above a certain threshold. cyclic/periodically running time shoots up- bit GC varies by a lot

### JVM Warmup
The demo showed two very different running times on two consecutive runs of the program. When a JVM program starts it undergoes a 
period of warmup, after which it achieves its maximum performance.

- first, the program is interpreted

- then, parts of program are compiled into machine code

- later, the JVM may choose to apply additional dynamic optimizations

- eventually, program will reach some steady state

#### ScalaMeter Warmers
Usually, we want to measure steady state program performance. ScalaMeter `Warmer` objectsd run the benchmarked code until detecting
steady state.

	import org.scalameter._
	val time = withWarmer(new Warmer.Default) measure
		(0 until 1000000).toArray

### Scala Configuration
ScalaMeter configuration clause allows specifying various parameters, such as the minumum and maximum number of warmup runs.

	val time = config(
		Key.exec.minWarmupRuns -> 20,
		Key.exec.minWarmupRuns -> 60
		Key.verbose -> true
	) with Warmer(new Warmer.Default) measure {(0 until 1000000).toArray}

### ScalaMeter Measurers
Finally, ScalaMeter can measure more than just running itme

- Measure.Default: plain running time

- IgnoringGC: running time without GC pauses

- OutlierElimination: removes statistical outliers

- emoryFootprint: Memory footprint of an object

- GarbageCollectionCycles: total number of GC pauses

- newer ScalaMeter versions can also measure method invocation counts and boxing counts
































