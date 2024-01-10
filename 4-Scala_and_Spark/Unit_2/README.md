# Reduction Operations & Distributed Key-Value Pairs
## Lecture 2.1 - Reduction Operations
### Transformations to Actions
Most of our intuition has focused on distributing transformations such as map, flatMap, filter, etc.

What about actions? In particular, how are common reduce- like actions distributed in Spark?

### Reduce Operations, Generally
Recall operations such as fold, reduce, and aggregate from Scala sequential collections. All of these operations have something in common (foldLeft, reduceRight, etc.).
Walks through a collection and combine neighboring elements of collection together to produce a singel combined result. 

#### Example

	case class Taco(kind: String, price: Double)
	val tacoOrder =
		List(
			Taco("Carnitas", 2.25)
			Taco("Corn", 1.75)
			Taco("Barbacoa", 2.50)
			Taco("Chicken", 2.00)
		)
	val cost = tacoOrder.foldLeft(0.0)((sum, taco) => sum + taco.price)

### Parallel Reduction Operations
the reduction operations needs to be of type (z: A)((A,A) => A): A, if its accumulating another type, i.e. type (z: B)((A, B) => B): B operation is nonparallelizable. This is the
primary distinction between fol- a parllelizable method and foldLeft (nonparallelizable). Foldleft applies a binary operator to a start value and all subsequent elements.
Recall non assocaitive operations on a collection of elements can not be parallelized. 

#### Aggregate
aggregate(z: => B)(seqop: (B, A) => B, combop: (B, B) => B): B, this method is parallelizable, because the combop tells us how to combine foldLeft shards. Seen as better than 
fold because we can change the return type and unlike foldLeft its parallelizable.

!()["pictures/vizual Parallelizable aggregate.png"]

## Lecture 2.2 - Pair RDDs
In single node Scala, key-value pairs can be thought of as maps- or associative arrays/dictionaries. Though available in most languages, not the most used structure in single
node programs. In Big data processing, manipulating key-value pairs is a key design of MapReduce. Largedatasets are often made of large complex, nested data records (think JSON)
To be able to work with such records, its desirable to project down these complex datatypes into key-value pairs. When working with distributed data useful to organixe into 
Key-value pairs- in spark pairs are Pair RDDs. This si useful because Pair RDDs allow you to act on each key in parallel or regroup data scross the network. Pair RDDs have
additional specialized methods for working with data associated with keys. RDDs are parameterized by a pair are Pair RDDs `RDD[(K,V)]`.

Imprtant extension methods for RDDs containing pairs

	def groupByKey(): RDD[(K, Iterable[V])]
	def reduceByKey(func: (V, V) => V): RDD[(K, V)]
	def join[W](other: RDD[(K, W)]): RDD[(K, (V, W))]

### Creating a Pair RDD
most often created from already-existing non-pair RDD's for example by using the map operaation on RDDs

	val rdd: RDD[WikipediaPage] = ...
	// type RDD[(String, String)]
	val pairRdd = rdd.map(page => (page.title, page.text))

Now you can do reduceByKey, groupByKey, join operations on this rdd

## Lecture 2.3 - Transformations and Actions on Pair RDDs
### Pair RDD transformation: groupBy
from Scala collections

	def groupBy[K](f: A => K): Map[K, Traversable[A]]

Partitions this traverable collection into a map of traversable colelctions according to some discriminator function. i.e. breaks up collecition into two or more according
to a function that you pass to it. Result of the function is the key, collection of results that return that key when the function is applied to it. Returns a Map mapping 
computed keys to collections of corresponding values. 

	val ages = List(2, 42, 53, 63, 42, 36, 78)
	val grouped = ages.groupBy {age =>
		if (age >= 18 && age < 65) "adult"
		else if (age < 18) "child"
		else "senior"
	}

	// Map[String, List[Int]] = HashMap(child -> List(2), adult -> List(42, 53, 63, 42, 36), senior -> List(78))

### groupByKey
similar, but takes a collection of Key Value pairs, and reduces to a singular key pointing to a collection of values

	case class Event(organizer: String, name: String, budget: Int)
	val eventsRdd = sc.parallelize(...)
			.map(event => (event.organizer, event.budget)
	val groupedRdd = eventsRdd.groupByKey()	

Does nothing till called, groupByKey is lazy- noice. if we call collect, result will be computed

	groupedRdd.collect()

### reduceByKey
can be thought of as a combination of groupByKey and reducing on all the values per key. More efficent than doing seperately, but same as calling groupByKey, then reduce

	def reduceByKey(func: (V, V) => V): RDD[(K, V)]

	val budgetsRdd = eventsRdd.reduceByKey(sum).collect() // can also use (_ + _) to denote suming arbitrary values

### mapValues

	def mapValues[U](f: V => U): RDD[(K, U)])

can be thought of as shorthand for

	rdd.map{ case (x, y): (x, func(y))}

That is it simply applies a function to only the values in a Pair RDD. **countByKey**() simply counts the number of elemnts per key in a pair RDD, returning a normal scala Map

	def countByKey(): Map[K, Long]

returns normal scala map- not rdd or parallelized.
Can also use a mapValues call to countByKey

	val intermediate =
		eventsRdd
			.mapValues(b => (b, 1))
			.reduceByKey((v1, v2) => (v1.budget+v2.budget, v1.organizer + v2.organizer))

	val avgBudgets = intermediate.mapValues(v => v._1/v._2).collect()

### Pair RDD transformation: Key
note this method is a transformation, its possible to have a single key map to a single value, hence the pair RDD can be unbounded; and not possible to collect on one node. 

	def keys: RDD[K]

Count the number of unique visitors to a website

	case class Visitor(ip: String, timestampL: String, duration: String)
	val visits: RDD[Visitor] = sc.textfile(...)
		.map(v => (v.ip, v.duration))
	val numUniqueVisits = visits.countByKey()

`keys.distinct().count()` is another way to collect the number of unique visits.

## Lecture 2.4 - Joins
These methods will be unique to pair RDD's. Another sort of transformation on Pair RDDs. They're used to combine multiple datasets They are one of the most commonly used 
operations on Pair RDDs. Two kinds of joins
- inner joins
- Outer joins (left, right outer)

#### Example
Let's pretend two datasets 1) represents customers and their subscribers 2) represents customers and cities they frequently travel to (locations). 

	val as = List(
		(101, ("Ruetli", AG)), (102, ("Brelaz", DemiTarif)),
		(103, ("Gress", DemiTarifVisa)), (104, ("Schatten", DemiTarif))
	)
	val abos = sc.parallelize(as)
	val ls = List(
		(101, "Bern"), (102, "Thun"),
		(103, "Chur"), (103, "Nyon"), 
		(105, "Char")
	)
	val locations = sc.parallelize(ls)

### Inner Join
returns combined Pair whose Keys are present in both input RDDs `def joiin[W](other: RDD[(K, W)]): RDD[(K, (V, W))]`. 

Back to example answers the question, how do we combine customers that have a subscription and where did they go?

	val trackedCustomers = abos.join(ls).collect()
	
	HashMap(101 -> (("Ruetli", AG), "Bern"), 102 -> (("Brelaz", DemiTarif), "Thun"),
		103 -> (("Gress", DemiTarifVisa), "Chur"), 103 -> (("Gress", DemiTarifVisa), "Nyon")
	)

### Outer Joins
return a new RDD containing combined pairs whose keys don't ahve to be present in both input RDDs. Outer joins are particuarly useful for customizing how the resulting joined RDD
deals with missing keys. With outer joins, we can decide which RDD's keys are most essential to keep- the left, or the right RDD in the join expression.

	def leftOuterJoin[W](other: RDD[(K, W)]): RDD[(K, (V, Option[W]))]
	def rightOuterJoin[W](other: RDD[(K, W)]): RDD[(K, (Option[V], W))]

Assume we want to know for which subscribers the CFF has managed to collect location information

	abos.leftOuterJoin(ls).collect()
	// HashMap(101 -> (("Ruetli", AG), "Bern"), 102 -> (("Brelaz", DemiTarif), "Thun"),
		103 -> (("Gress", DemiTarifVisa), "Chur"), 103 -> (("Gress", DemiTarifVisa), "Nyon"),
		104 -> (("Schatten", DemiTarif), None)
	)

Want customers that use smartphone and their commutes

	abos.rightOuterJoin(ls).collect()
	// HashMap(101 -> (("Ruetli", AG), "Bern"), 102 -> (("Brelaz", DemiTarif), "Thun"),
		103 -> (("Gress", DemiTarifVisa), "Chur"), 103 -> (("Gress", DemiTarifVisa), "Nyon"),
		105 -> (None, "Char")
	)

