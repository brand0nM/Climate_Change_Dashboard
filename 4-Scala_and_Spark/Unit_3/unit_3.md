# Partitioning and Shuffle
## Lecture 3.1 - What is it and why is it important
Think again what happens when you have a groupBy  or groupByKey operation- remember daat is distributed. 

looking at just a groupByKey() object will produce a `shuffledRDD`. Typically will have to move data from one node 
to another to be "grouped with" its key. This is called shuffling. I.e. we will have to move data from one node to 
another to be grouped with which can produce a lot fo latency.

	case class CFFPurchase(customerId: Int, destination: String, price: Double)

assume have an RDD of the purchases that users of the Swiss train company's mobile app have made in the past month

	val purchasesRDD: RDD[CFFPurchse] = sc.textFile(...)

calculate how many trips and how much money was spent by each individual customer over the course of the month. 

	val purchasesPerMonth = purchasesRDD
		.map((_.customerID, _.price)))
		.groupByKey()
		.mapValues(arr => (arr.length, arr.sum))

!()["pictures/vizual  groupby reduce.png"]

Recall: too much communication will kill performance

To navigate these problem can use the reduce amount of data need to send over network.

### ReduceByKey
a combination of groupByKey then reducing on all values grouped per key.

	def reduceByKey(func: (V, V) => V): RDD[(K, V)]

	val purchasesPerMonth = purchasesRDD
		.map((_.customerID, (1, _.price)))
		.reduceByKey((v1, v2) => (v1._1+v2._1, v1._2+v2._2))

!()["pictures/vizual reduceBy Key.png"]

Now will shuffle less data => reduces the amount of network traffic.

Partitioning will determine which key-value pair should be on which machine

## Lecture 3.2 - Partitioning
Data withing RDD is split into several partitions
- never span multiple machines, i.e. tuples in the same partition are guaranteed to be on the same machine
- each machine in the cluster contains one or more partitions
- number of partitions to use is configurable. by default, it equals the total number of cores on all executor nodes

Types
- Hash
- Range

customizing a partition is only possible on Pair RDDs

### Hash Partitioning
`groupByKey`, will first compute per tuple (k, v) its partition p:

	p = k.hashCode() % numPartitions

then all tuples in same partition are sent to machine hosting p. Attempts to spread data evenly across partitions 
based on key.

#### Example
with keys [8, 96, 240, 400, 401, 800] and a desired number of partitions of 4.
Suppose hashCode() is identity (n.hashCode() == n)
- partition0: [8, 96, 240, 400, 800]
- partition1: [401]
- partition2: []
- partition3: []

result is a very unbalanced distribution which will hurt performance

### Range Partitioning
may contain keys that have an ordering defined. For such range partitioning may be more efficient. 
Partitioned according to:
1) an ordering for keys
2) a set of sorted ranges of keys

Property: tuples with keys in the same range appear on the same machine

#### Example using range to improve
set of ranges [1, 200], [201, 400], [401, 600], [601, 800]
- partition0: [8, 96]
- partition1: [240, 400]
- partition2: [401]
- partition3: [800]

result becomes more balanced. 

### Partitioning Data
How to set a partitioning for our data
1) Call partitionBy on an RDD, rpovifing an explicit Partitioner

2) Using transformations that return RDDs with specific partitioners

#### partitionBy 
method creates and RDD with a specific partitioner

	val pairs = purchasesRdd.map(p => (p.customerId, p.price))
	// will use pairs to determine the 8 best ranges
	val tunedPartitioner = new RangePartitioner(8, pairs)
	// call to persist will write to memory and only compute operation once
	val partitioned = pairs.partitionBy(tunedPartitioner).persist()

`rangePartitioner`
- specify the desired number of partitions
- provide a pair RDD with ordered keys. This RDD is sampled to create a suitable set of sorted ranges

results of partionBy should always be persisted to only compute once (involves shuffle each time)

Using Transformations:
- parentRDD: pairs are the result of a transformation on a partitioned Pair RDD, typically configured to use the 
hash partitioner that was use to construct it.
- Automaticallyset partitioner: some operations on RDDsresult in an RDD with a known partitioner
 - when using sortByKey, rangePartitioner is used. Further default for groupByKey is a HashPartitioner

#### Partitioning Data using transformations
Operations on Pair RDDs that hold to (and propagate) a partitioner:
- cogroup
- groupWith
- join
- leftOuterJoin
- rightOuterJoin
- groupByKey
- reduceByKey
- foldByKey
- combineByKey
- partitionBy
- sort
- mapValues (if parent has partitioner)
- flatMapValues (if parent has partitioner)
- filter (if parent has partitioner)

all other results will produce a result without a partitioner

why not map? Consider

	rdd.map((k, v) => ("ohhno", v))

Thus `mapValues` is best, it enables us to still do map transformations without changing the keys, therbey 
preserving partitioner.

## Lecture 3.3 - Optimizing with Partitioners
Will to discuss why would want to repartition. Bring performance gains, when facing lots of shuffle

### Optimization using range partitioning
optimize use of reduceByKey so no network shuffle

	val pairs = purchasesRdd.map(p => (p.customerId, p.price))
	val tunedPartitioner = new RangePartitioner(8, pairs)
	val partitioned = pairs.partitionBy(tunedPartitioner).persist()
	val purchasesPerCust = partitioned.map(p => (p._1, (1, p._2)))

Slowest

	val purchasesPerMonth = partitioned
		.map(p => (p.customerId, p.price))
		.groupByKey()
		.map(p => (p._1, (p._2.size, p._2.sum)))
		.count()

Still Slow

	val purchasesPerMonth = purchasesPerCust
		.reducebyKey((v1, v2) => (v1._1+v2._1, v1._2 +v2._2))
		.count()

Fastest

	val purchasesPerMonth = partitioned
		.map(x => x) // ???
		.reducebyKey((v1, v2) => (v1._1+v2._1, v1._2 +v2._2))
		.count()

Consider an application that keeps a large table of user information in memory
- userData: Big, containing (UserID, UserInfo) pairs, where UserInfo contains a list of topics the user is 
subscribed to

Periodically combined events to big table every five minutes
- events: small containing (UserID, LinkInfo) for users who have clicked a link on a awebsite in those five minutes

Now we may want to count how many users visited a link that was not to one of their subscribed topics. Can perform a
join between the UserInfo and LinkInfo pairs on each UseerID

	val sc = new SparkContext(...)
	val userData = sc.sequenceFile[UserID, UserInfo]("hdfs://...").persist() // cash hadoop data from memory
	def processNewLogs(logFileName: String)
		val events = sc.sequenceFile[UserID, LinkInfo](logFileName)
		val joined = userData.join(events)
		val offTopicVistits = joined.filter{
			case (userID, (userInfo, linkInfo)) =>
				!userInfo.topics.contains(linkInfo.topic)
		}.count()

Horribly inefficeint because each time processNewLogs is invoked, has no data on keys partition.
Will default to operating on the hash of all keys in both datasets- vice taking a subset of userData that have had
events in the past 5 minutes. Can fix by partitioning big Rdd

	val userData = sc.sequenceFile[UserID, UserInfo]("hdfs://...")
		.partitionBy(new HashParitioner(100))
		.persist()

Spark will now now its hash partitioned and will only send events to relevant nodes that contain corresponding has 
partition. Opposed to shuffleing large RDD data

groupByKey, will by deffault use a hash partitioner with default parameters to collect all key value pairs on the
same machine. The result RDD, is configured to use the hash partioner that was used to construct it.+

**Rule of Thumb** shuffle can occur when the resulting RDD depends on other elements from the same RDD to another.

### How to know if Shuffle will Occur
1) look at return type of specific transformation should be a `ShuffledRDD[366]`

2) Using dunction to `DebugString` to see its execution plan on transformation

#### Operations that might cause a shuffle
- cogroup
- groupWith
- join
- leftOuterJoin
- rightOuterJoin
- groupByKey
- reduceByKey
- combineByKey
- distinct
- intersection
- repartition
- coalesce

#### Avoiding a Netwrok Shuffle By Partitioning
Few ways to use operations that might cause shuffle and to still avoid much or all network shuffling

1) reduceByKey running on a pre-partitionedRDD will cause values to be computed locally requiring only the final 
reduce value to be sent from worker to driver

2) join called on two RDDs that a pre-partitioned with the same partitioner and cached on the same machine will cause
the join to be computed locally with no shuffling across network

## Lecture 3.4 - Wide vs Narrow Dependencies
Some transformations wil have significantly more latency (and be more expensive) than others

Will look at
- how toRDDs are represented
- how and when Spark decides to shuffle data
-see how these dependencies make fault tollerance possible

### Lineage
Computations on RDDs are represnted as a lineage graph (DA)

	val rdd = sc.textFile(...)
	val filtered = rdd.map(...)
		.filter(...)
		.persist()
	val count = filtered.count()
	val reduced = filtered.reduce(...)

!()["pictures/ex% dag.png"]

### How are RDDs represented
RDDs are made of 2 important parts
- partitions: atomic pieces of the data one or many compute nodes
- Dependancies: model relation between RDD and parent from which it was derived. more specifically which partition 
maps from its parent
- Function: for computing the dataset based on its parent rdd
- metadata: about partitioning scheme and data placement

!()["pictures/vizual% rdd.png"]

### RDD Dependencies and Shuffles
shuffle can occurw hen the resulting RDD depnds on other eleemnts from the same RDD or another. RDD dependencies 
encode when data must move across the network
**Transformations cause shuffles** with 2 types of dependencies

1) Narrow Dependencies: each parition of the parent RDD is used by at nmost one partition of the child RDD
*fast require no shuffles*

2) Wide Dependencies: Each partition of the parent RDD may be dependend on by multiple child partitions
*slow require all or some data to be shuffled*

#### Narrow Dependencies
- map
- mapValues
- flatMap
- filter
- mapPartitions
- mapPartitionWithIndex
!()["pictures/rdd% narrow% dependencies.png"]

#### Wide Dependencies
- cogroup
- groupWith
- join (inputs not copartitioned)
- leftOuterJoin
- rightOuterJoin
- groupByKey
- reduceByKey
- combineByKey
- distinct
- intersection
- repartition
- coalesce
!()["pictures/rdd% wide% dependencies.png"]

#### Example
!()["pictures/ ex% bottleneck% rdd.png"]

Wide Dependencies: A -> B, F -> G

Narrow Dependencies: C -> D, D -> F, E -> F, B -> G

so the groupBy and join operation can bottleneck pipeline (if partions are not carefully chosen

### How can I find out
dependencies method on RDDs. returns a sequence of depenceny objects which are actually dependencies used by Spark's
scheduler to know how this RDD depends on other RDDs

Narrow
- OnToOneDependency
- PruneDependency
- RangeDependency

Wide 
- ShuffleDependency

toDebugString method on RDDs prints out a visulaization of the RDD's lineage

### Lineages and Fault Tolerance
Ideas from functional programming enable fault tolerance in Spark
- RDDs are immutable
- use higher order functions like map, flatMap, filter to do functional transformations on this immutable data
- function for computing the dataset based on its parent RDDs also is part of an RDD representation

This is how Spark can achieve fault tolerance without writing to disk. Recover failures by recomputing lost
partitions in lineage graph

recomputing missing partitions is fat for narrow dependencies but slow for wide( since has to recompute the whole
RDD and not a singular partition)
