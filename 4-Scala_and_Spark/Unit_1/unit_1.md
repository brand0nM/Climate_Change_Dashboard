# Spark Basics
## Lecture 1.1 - From Parallel to Distributed
- About Distributed data parallelism in Spark
- Extending familiar functional abstractions like functional list over large clusters
- analyzing large data sets

### Why Scala, Why Spark
*Normally*: Data Science and Analytics is done in R, Python, Matlab, etc for the smaller data sets. If your dataset ever gets too large to fit into memory, 
these languages/frameworks won't allow you to scale. You've to reimplement everything in some other language or system. 

- Spark advantages:
 - API's modeled after scala collections. Look like functional lits. Richer, more composable operations possible than in MapReduce
 - Performance: not only in terms of runtime, also in terms of developer productivity. Interactive
 - Good for data science. not just because of performance but because it enables iteration, which is required by most algorithms in a data scientist toolbox

What We'll Leran:
- Extending data parallel paradigm to the distrubuetd case, using spark
- spark's prgramming model
- distributed computation and cluster topology in spark
- how to improve performance; data locality, how to avoid recomputation and shuffle in spark
- relational operations with dataframes and datasets

Books and resources
- learning spark
- spark in action
- high performance spark
- advanced analytics with spark

## Lecture 1.2 - Data Parallel to Distributed Data Parallel
### Vizualizing Shared memory Data Parllelism
- split the data
- workers/threads independently operate on the data shards in parallel
- combine when done

!()["pictures/shared% memory% parallelism.png"]

### Vizuaklizing Distributed Data Parallelism
- split the data over severl nodes
- nodes independently operate on the data shards in parallel
- combine when done

**Now have to worry about network latency between workers**

!()["pictures/Multinode% Paralellism.png"]

*recall*: be careful of nonassociative operations operating in parallel

## Lecture 1.3 - Latency
### Distribution
important concerns
- Partial Failure: crash failure on a node in cluster
- latency: certain operations can have higher latency because they require more network communication

#### Latency
!()["pictures/important latency numbers.png"]

### Hadoop/MapReduce
Open source implementation of googles mapreduce
- a simple API (simple map and reduce steps)
- fault tollerance (data redundancy across nodes); likelyhood of at least one machine failing is very high, but allowed for configuartions of 100-1000's of 
nodes

Fault Tollerance comes at a cost: between each map reduce step, in order to recover failures, hadoop shuffles its data and writes intermediate results to disk.

### Spark 
- retains the idea of fault tollerance
- different strategy for handling latency

*idea* keep all data immutable in memory. All operations on data are just functional transformations, like regular scala collections. Fault tolerance is 
achieved by replaying functional transformations over original dataset => at times 100x more performant than hadoop clusters

## Lecture 1.4 - RDD's Spark's Distributed Collection
### Resilient Distributed Datasets
On surface seem a lot like uimmutable sequential or parallel scala collections

	abstract class RDD[T]
		def Map[U](f: U => T): RDD[U] = ...
		def flatMap[U](f: T => TraversableOnce[U]): RDD[U] = ...
		def filter(f: T => Boolean): RDD[T] = ...
		def reduce(f: (T,T) => T): RDD[T] = ...

Most operations on RDD's like Scala's immutable List and Parallel collections are higher order functions. This is metyhod that works on RDDs taking function 
as arguments and which typically return RDD's

Core API's avaiable on sequential collections are available for RDD's

**Use Case**: given val encyclopedia: RDD[String], want to search all of encyclopedia for metions of EPFL, adn count the number of pages that metion EPFL.

	val Mentions: Int = encyclopedia.filter(_.contains("EPFL")).length

	// Create RDD, where each String represents a line of text
	val rdd: RDD[String] = spark.textFile("hdfs://...")
	val count = rdd.flatMap(_.split(" ")).length

### How to create an RDD
- transforming an existing RDD
- from a SparkContext or SparkSession object

**SparkContext or SparkSession Object**: can be thought of as the handle to your spark cluster. Represents the connection between spark cluster and running application. Defines handful of methods which can be used to create and popuklate a new RDD
- parallelize: convert a local scala collection to an RDD
- textFile: read a text file from HDFS or a local file systme and return an RDD of String

## Lecture 1.5 - RDDs Transformation and Actions
recall on Parallel and Sequential collections:
- Transformers; return new collections as results EX: map, filter, flatMap, groupBy (map(f: A => B): Traversable[B])
- Accessors; return single value EX: aggregate, fold, reduce (reduce(op:(A,A) => A): A)

on RDDs:
- Transformers: Return a new RDD collection as result; they are lazy, results are not immediately computed
- Action: Compute a result based on an RDD, either returned or saved to an external storage system; They are eager, result is immediately computed

#### Examples
Consider the following simple example

	val largeList: List[String]: ...
	val wordsRdd = sc.parallelize(largeList)
	val lengths Rdd = wordsRdd.map(_.length)

Nothing has happended yet, add an action to compute result

	val totalChars = lengthRdd.reduce(_ + _)

### Common Eager Actions
- collect(): Array[T]; return all elemts of RDD as an Array
- count
- take(num: Int): Array[T]; array length 100 type T
- reduce
- foreach

#### Example
Determine how many errors were logged in December 2016 RDD[String], prefixed "error" followed by DT stamp

	val lastYearsLogs: RDD[String] = list.filter(_.contains("error2016-12")).count()
	val firstTenLastYearsLogs: RDD[String] = list.filter(_.contains("error2016-12")).take(10)

spark can leverage laziness and optimize the chain of operations, I.E. only computing an RDD of 10 elements before returning

### Special RDD Transformations
combine two rdd's
- union
- intersection
- subtract
- cartesian

### Useful RDD Actions
contain other important actions unrelated to regulaar Scala colelctions, but which are useful when dealing with distributed data
- takeSample(withRepl: Boolean, num: Int): Array[T]
- takeOrdered(num: Int)(implicit ord: Ordering[T]): Array[T]
- saveAsTextFile(path: String): Unit, file located in HDFS
- saveAsSequenceFile(path: String): Unit; file located in HDFS

## Lecture 1.6 - Evaluations in Spark: Unlike Scala Collections
### Why is Spark good for Data Science
Let's start by recapping some major themes from previous sessions
- we learned the difference between transformations and actions
 - transformations: Deffered/lazy
 - actions: Eager, kick off staged transformations

- we learned that latency makes a big difference; too much wastes the time of data analyst
 - in memory computation: Significantly lower latencies (several orders of magnitude)

### Iteration and Big Data Processing
Iteration in Hadoop:

Input (from HDFS) -(File system read)-> iteration 1 -(file system write)-> Read/Write inntermediate -(file system read)-> iteration 2 
-(file system write)-> ...

Iterations in Spark:

Input (from HDFS) -(file system read)-> Iteration 1 -> iteration 2 -> iteration 3 -> ...

### Iteration, Example: Logistic Regression
Logistic Regression is an iterative algorithm typically used for classification. Like other classification algorithms, the classifiers weights are iteratively
updated based on a training dataset. `w <- w - a* Sum (i, n) g(w; xi, yi)`

Logistic regression is an iterative algorithm typically used for classification. Like other classification algorithms, the classifier's weights are iteratively
updated based on a training dataset.

	val points = sc.textFile(...).map(parsePoint)
	val w = vector.zeros(d)
	for (i <- 1 to numIterations) {
		val gradient = points.map { p =>
			(1 / (1 + exp(-p.y * w.dot(p.x))) - 1) * p.y * p.y
		}.reduce(_ + _)
		w -= alpha * gradient
	}

### Caching and Persistence
By default, RDDs are recomputed each time you run an action on them. This can be expensive (in time) if you need to use a dataset more than once.
Spark allows you to control what is cached in memory.

	val lastYearsLogs: RDD[String] = ...
	val logsWithErrors = last yearsLogs.filter(_.contains("ERROR")).persist()
	val firstLogsWithErrors = logsWithErrors.take(10)
	val numErrors = logsWithErrors.count()

Now, computing the count on logsWithErrors is much faster.

### Back to our Logistic Regression Example
Logistic regression is an iterative algorithm typically used for classification. Like other classification algorithms, the classifier's weights are iteratively
updated based on a training dataset.

	val points = sc.textFile(...).map(parsePoint)
	val w = vector.zeros(d)
	for (i <- 1 to numIterations) {
		val gradient = points.map { p =>
			(1 / (1 + exp(-p.y * w.dot(p.x))) - 1) * p.y * p.y
		}.reduce(_ + _)
		w -= alpha * gradient
	}

Now, points is evaluated once and is cached in memory; It is then reused on each iteration.

### Caching and Persistence
There are many ways to configure how your data is persisted.

Possible to persist data set:
- in memory as regular Java objects
- on disk as regular Java objects
- in memory as serialized Java objects (more compact)
- on disk as serialized Java objects (more compact)
- both in memory and on disk (spill over to disk to avoid re-computation)

**cache** shorthand for using the default storage level which is in memory only as regular Java objects

**persist** Persistence can be customized with this method. Pass the storage level you'd like as a parameter to persist.

Declarations you can make to tell spark where to cache data
!()["pictures/caching commands.png"]

### RDDs look like collections, but have totally different
Takeaways:
Despite similar looking API to Scala Colelctions (the deferred semantics of Spark's RDD are very unlike Scala collections

Due to:
- the lazy semantics of RDD transformation operations (map, flatMap, filter)
- and users' implicit reflex to assume collections are eagerly evaluated

One of the most common performance bottlenecks of newcommers to spark arise from unknowningly reevaluating several transformations when chaching could be used.

### Restating the Benefits of Laziness for Large-Scale Data
While many users struggle with the lazy semantics of RDDs at first, helpful to remeber the ways in which these semantics are heklpful in the face of large
scale distributed computing

#### Example

	val lastYearsLogs: RDD[String] = ...
	val firstLogsWithErrors = lastYearsLogs.filter(_.contains("ERROR")).take(10)

The execution of filter is deffered until the take action is applied. Spark leverages this by analyzing and optimizing the chain of operations before executing
it. Spark will not compute intermediate RDDs. Instead, as soon as 10 elements of the filtered RDD have been computed, firstLogsWithErrors is done. At this 
point Spark stops working, saving time and space computing elements of the unused result of filter.

	val numErrors = lastYearsLogs.map(_.lowercase)
				.filter(_.contains("error"))
				.count()

Lazy evaluation of these transformations allows Spark to stage computations. That is Spark can make important optimizations to the chain of operations before
execution. For example, after calling map and filter, Spark knows that it can avoid doing multiple passes through the data. That is, Spark can traverse through
the RDD once, coopmuting the result of map and filter in this single pass, before returning the resulting count.

## Lecture 1.7 - Cluster Topology Matters
### A simple println
let's start with an example Assume we have RDD populated with Person objects

	case class Person(name: String, age: Int)

What does the following code snippet do?

	val people: RDD[Person] = ...
	people.foreach(println)
	val first10 = people.take(10)

Where will the Array[Person] representing first10 end up?
It will be cached to memory- unless dataset is too large -> then will spill to disk

### How Spark jobs are Executed
i.e. Cluster manager Yarn, Mesos; Allocates resources across cluster manages scheduling

		[Driver Program (Spark Context)]
		               |               
		       [Cluster Manager]
		      /                 \
	[Worker Node(Executor)] [Worker Node(Executor)]

A Spark application is a set of processes running on a cluster.
All these processes are coordinated by the driver program
- the process where the main() method of your program runs
- the process running the code that creates a SparkConetxt, creates RDDs, and stages up or sends off transformation and actions

These processes that run computations and store data for your application are executors
- Run the tasks that represent the application
- return computed results to the driver
- Provide in-memory storage for cached RDDs

Execution fo aa spark program
- Driver program runs the spark application, which creates a SparkContext upon Start-up
- The SparkContext copnnects to a cluster manger (e.g. Mesos/Yarn) Which allocates resources
- Spark aquires executors on nodes in the cluster, which are processes that run computations and store data for you application
- Next, driver program sends your application code to the executors
- Finally, SparkContext sends tasks for the executors to run

### A Simple println
Let's start with an example. Assume we have an RDD populated with Person objects

	case class Person(name: String, age: Int)

What does the following code snippet do?

	val people: RDD[Person] = ...
	val first10 = people.take(10)

Where will the Array[Person] representing first10 end up? 
**The driver program**; executing an action invloves communication between worker nodes and the node running the driver program.

Due an API which is mixed eager/lazy, its not always immediately obvious upon first glance on what part of the cluster a line of code might run on. It's on
you to know where your code is executing.









































































































