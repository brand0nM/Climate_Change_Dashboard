# Structured Data: SQL, Dataframes, and Datsets
## Lecture 4.1 - Structured vs Unstructured Data
#### Example Selecting Scholarship Recipients
Imagine we are oganization, offering scholarships to programmers and datsets

	case class Demographic(
		id: Int, 
		age: Int,
		codingBootcamp: Boolean,
		country: String,
		gender: String,
		isEthnicMinority: Boolean,
		servedInMilitary: Boolean)
	val demographics = sc.textfile(...)...
	
	case class Finances(
		id: Int, 
		hasDebt: Boolean,
		hasFinancialDependents: Boolean,
		hasStudentLoans: Boolean,
		income: Int)
	val finances = sc.textfile(...)...

Lets count
- swiss students
- who have debt and financial dependents

	finances
		.join(demographics)
		.persist()
		.filter( p =>
			p._2._1.country=="Switzerland" &&
			p._2._2.hasFinancialDependents &&
			p._2._2.hasDebt)
		.count()

Steps:
1) Inner join first

2) Filter to select people in Switserland

3) Filter to select people with debt & financial dependents

	val filtered = finances
		.filter(p => p._2.hasFinancialDependents && p._2.hasDebt)
	
	demographics
		.filter(p => p._2.country == "Switzerland")
		.join(filtered)
		.count()

Method 3

	val cartesian = demographics
		.cartesian(finances)
	cartesian.filter{
		case (p1, p2) => p1._1 == p2._1
	}
	.filter{
		case (p1, p2) =>
			(p1._2.country == "Switzerland") &&
			(p2._2.hasFinancialDependents) &&
			(p2._2.hasDebt)

	}.count()

Filtering first is substantially faster. 
Cartesian product is extremely slow..

### Structured vs Unstructured
All data isn't equal, structurally. Falsl on a sprectrum from unstructured to structured

Log files, Images -> JSON, XML -> Database Tables

### Structured Data vs RDDs
Spark + regular RDD's dont know anything about the schema of data its dealing with. Given an arbitrary RDD, Spark knows that the RDD is parameterized with arbitrary types
such as
- Person
- Account
- Demographics

Doesn't know anything about these type's structure

### Structured Data vs RDDs
Assuming we have a dataset Account objects: `case class Account(name: String, balance: Double, risk: Boolean)` 

Spark/RDD[Account]: Blobs of objects we know nothing about, except that they're called Account. Spark can't see inside this object or analyze how it may be used, and to 
optimize based on that usage. It's opaque.

Database/Hive sees: columns of anmed and typed values. If Spark could see data this way, it could break up and only select the datatypes it needs to send around the 
cluster.

### Structured vs Unstructured Computation
The same can be said about computation. In Spark:
- we do functional transformations on data
- pass user-defined function literals to higher order functions like map, flatMap, filter

Like the data Spark operates on, function literals too are completely opaque to Spark. A user can do anything inside one these, and all Spark can see is something 
Spark can't optimize on

In Hive, do declarative transformations on data, specialized/structured, predefined operations. Fixed set of operations fixed set of types they operate on. Optimizations 
the norm. 

### Optimizations + Spark
RDDs operate on unstructured data, and there are few limits on computation; your computations are defined as functions that you've written yourself, on your own data types
But as we saw have to do all optimization work ourselves. Wouldnt it be nice if Spark could do some of these optimixations for us- Sprak SQL makes this possible. 

## Lecture 4.2 - Spark SQL
### Relational Database
Pain to connect big data pipelines to spark, can use SQL scripting in spark though. Spark SQL delievers both

three main goals
- support relational processing within Spark programs on RDDs and on external data sources with a friendly API
- High performance, achieved by using techniques from research in databases
- easily support new data sources such as semi-structured data and external databases

### Spark SQL
- its a spark module for structured data processing
- it is implemented as a library on top of Spark

Three main APIs it adds
- SQL literal syntax
- Dataframes
- Datasets

Two specialized backend components
- Catalyst, query optimizer
- Tungsten, off-heap serializer

Spark SQL's dataframe API though distinct is comprised and run off RDD's

### Relational Queries (SQL)
Everything about SQL is structured. In fact, SQL stands for structual query language
- There are a set of fixed data types. Int, Long, String, ...
- There are fixed set of operations. Select, where, group by, etc.

Research and industry surrounding relational databases has focused on exploiting this rigidness to get performance speedups

Dataframes are copnceptually RDDs full of records with a known schema

Dataframe is Spark SQL's core abstraction, are untyped, conceptually, RDDs full of records with a known schema. Transformations on Dataframes are also known as untyped
transformations.

### SparkSession
To get started using Spark SQL, everything starts with SparkSession

	import org.apache.spark.sql.SparkSession

	val spark = SparkSession
		.builder()
		.appName("App")
		.getOrCreate()

### Creating Dataframe
1) using an existing RDD. Given pair RDD[(T1, T2, ..., TN)] a dataframe can be created with its schema automatically inferred by simply using the toDF method

	val tupleRDD = ...
	val tupleDF = tupleRDD.toDF("id","name","city","country")

If already have an RDD of case classes can just call to DF and column names will become class field names; this is an example of reflections

	case class Person(id: int, name: String, city: String)
	val peopleRDD = ...
	val peopleDF = peopleRDD.toDF

Explicity Specified schema

	val schemaString = "name age"
	val fields = schemaString.split(" ")
		.map(fieldName => StructField(fieldName, StringType, nullable = true))
	val schema = StructType(fields)
	val rowRDD = peopleRDD
		.map(_.split(","))
		.map(attributes => Row(attributes(0), attributes(1).trim))
	val people DF = spark.createDataFrame(rowRDD, schema)
	
2) reading in specified data source from file. Schema explicity specified. SOmetimes its not possible to create a dataframe with a predetermined case class as its schema.
For these cases, tis possible to explicity specify a schema. It takes three steps:
- Create an RDD of rows from the original RDD
- Creates the schema represented by a StructType matching the structure of Rows in the RDD created previously
- Apply the schema to the RDD of Rows via createDataFrame method provided by SparkSession

	case class Person(name: String, age: Int)
	val peopleRdd = sc.textFile(...)

Read in datasource file, i.e. JSON and infer schema (JSON, CSV, Parquet, JDBC)

	val df = spark.read.json("examples/.../people.json")

### SQL Literals
Once you have a Dataframe to operate on, you can now freely write familiar SQL syntax to operate on your dataset!

Given: A dataframe called peopleDF, we just have to register our DataFrames as a temporary SQL view first

	peopleDF.createOrReplaceTempView("people")
	val adultsDF = spark.sql("SELECT * FROM people WHERE age > 17")

### A more interesting SQL Query
Let's assume we have a DataFrame representing a dataset of employees:

	case class Employee(id: Int, fname: String, lname: String, age: Int, city: String)
	val employeeDF = sc.parallelize(...).toDF

Want ID's and last names of employees working in Syney, and sort in increasing employee ID

	spark.sql("SELECT id, lname FROM employees WHERE city = 'Sydney' ORDER BY id")

## Lecture 4.3 - DataFrames
So far, we got an intuition of what DataFrame are, and we learned how to create them. We also saw that if we have a DataFrame, we use SQL syntax and do SQl queries on them
DataFrames have their own APIs as well. Focus on DataFrames API. We'll dig into
- available DataFrame data types
- some basic operations on DataFrames
- aggregations on DataFrames

### In a Nutshell
**Dataframes are...**

Relations API over Spark's RDDs: can be more convenient to use declarative relational APIs than functional APIs for analysis jobs

Able to be automatically aggressively optimized: Spark SQL applies relational optimizations in the databases community to Spark

Untyped: Elements within DataFrames are Rows, which are not parameterized by a type. Therefore, compiler cannot check Spark SQL schemas in DataFrames

### Dataframes Data Types
!()["pictures/Dataframe types.png"]

Complex Spark SQL Data Types:
- Array[T]: ArrayType(elementType, containsNull)
- Map[K, V]
- case class

if Arrays  `containsNull` is set to true if the elements in ArrayType value can have null values.

	case class Person(name: String, age: Int)
	StructType(
		List(
			StructField("name", StringType, true),
			StructField("age", IntegerType, true)
		)
	)

!()["pictures/combining complex datatypes.png"]

### Accessing Spark SQL Types
In order to acces any data types, either basic or complex, you must first import Spark SQL types

	import org.apache.spark.sql.types._

### DataFrames Operations Are More Structured
When introduced, teh dataframes api introduced a number of relational operations. DataFrame APIs accept Spark SQL expressions, instead of arbitrary user-defined function
literals like we were uesd to on RDDs. Allows the optimizer to understand what the computation represents and with filter it can often be used to skip reading unnecessary 
records.

Methods for DataFrames API
- select
- where
- limit
- orderBy
- groupBy
- join

### Getting a look at the data
- Show(); first 20 elements, can add arg to specify how many to show
- printSchema(); prints schema of your DataFrame in a tree format

	case class Employee(id: Int, fname: String, lname: String, age: Int, city: String)
	val employeeDF = sc.parallelize(...).toDF
	employeeDF.printschema()

Like on RDDs transformations on DataFrames are

1) operations which return a DataFrame as a result

2) Are lazily evaluated

Some common transformations include:

	def select(col: String, cols: String*) DataFrame
	def agg(expr: Column, exprs: Column*): DataFrame
	def groupBy(col1: String, cols: String*): DataFrame
	def join(right: DataFrame): DataFrame

### Specifying Columns
can modify using implicits `df.filter($"age">18)`, reference DF `df.filter(df("age" > 18))`, or SQL query `df.filter("age > 18")`.
Solve previous problem using DataFrame API

	val adultsDF = employeeDF
		.select("id", "lname")
		.where(employeeDF("city")== "Sydney")
		.orderBy("id")

Using filter or where

	val over30 = employeeDF.filter("age > 30").show()
	val over30 = employeeDF.where("age > 30").show()
	employeeDF.filter(($"age">25) && ($"city" === "Sydney")).show()

### Grouping and Aggregating on DataFrames
For grouping and aggregating Spark SQL provides
- a groupby function which returns a RelationalGroupedDataSet
- which has several standard aggregation functions defined on it like count, sum, max, min and avg

	case class Listing(street:String, zip: Int, price: Int)
	val listingsDF = ...
	import org.apache.spark.sql.functions._
	val mostExpensiveDF = listingsDF.groupBy($"zip").max("price")
	val leastExpensiveDF = listingsDF.groupBy($"zip").min("price")

## Lecture 4.4 - DataFrames 2
- working with missing values
- common actions on dataframes
- joins on dataframes
- optimizations on dataframes

### Cleaning Data With dataframes
Sometimes you have a data set with null or Nan values. In these cases its often desirable to do one of the following
- drop rows/records with unwanted values like null or naN
- replace certain values with a constant

#### dropping records with unwanted values
- drop(), removes rows that contain null or NaN values in any column
- drop("all"), removes rows that contain all null or NaN values
- drop(Array("id","name")) onlt drop rows taht contain null in these columns

#### replacing unwanted values
- fill(0) replaces all occurances of null or NaN in numeric columns with specific values and returns a new dataframe
- fill(Map("MinBalance" -> 0)) replaces all occurances of null or NaN in specified couln with specified value
- replace (Array("id"), Map(1234, 5678)) replaces specified value in column id with specified replacement and returns new dataframe

### Common Actions on DataFrames
Like RDDs Dataframes have their own set of actions
- collect(): Array[Row], array of all rows in  the dataframe
- count(): returns the number of rows in the dataframe
- first(): Row/head(): Row, returns the first row in dataframe
- show(): Unit, displays the top 20 rows of dataframe in tabular form
- take(n: Int): Array[Row], returns first n rows in dataframe

### Joins on DataFrames
Have to specify which columns to join on
(inner, outer, left_outer, right_out, leftsemi)

	df1.join(df2, df1("id") === df2("id"), "right_outer")
	
since same column exists in both

	df1.join(df2, Seq("id"), "right_outer")

#### Example

	case class Abo(id: Int, v: (String, String))
	case class Loc(id: Int, v: String)

	val as = List(
		Abo(101, ("place1", "country")), Abo(102, ("place2", "country"))
		Abo(103, ("place3", "country")), Abo(104, ("place4", "country"))
	)
	val abosDF = sc.parallelize(as.).toDF
	val ls = List(
		Loc(101, "WhereGo"), Loc(101, "WhereGo"), Loc(102, "WhereGo"), Loc(10, "WhereGo")
		Loc(1012 "WhereGo"), Loc(103, "WhereGo"), Loc(103, "WhereGo"), Loc(103, "WhereGo")
	)
	val locationsDF = sc.parallelize(ls).toDF

How to combine customers that have subscription and their location info

	val trackedCustomersDF = abosDF.join(locationsDF, Seq("id"))

Want to know which subscribers collected location information,(i.e. possible for someone to travel, doesn't use CFF app, buys tickerts in cash

	locationsDF.join(abosDF, Seq("id"), "right_outer")

customer 104 returned null and can target for Mobile app ads

### Revisisting Our Selecting Scholarship Recipients Example
Now that we're use DataFrames API, revisist example that we looked at

Imagine Code Award scholarship

	case class Demographics(
		id: Int, age: Int
		codingBootcamp: Boolean,
		country: String, gender: String,
		isEthnicMinority: Boolean, servedInMilitary: Boolean)
	val demographicsDF = sc.textfile(...).toDF
	case class Finances(
		id: Int,
		hasDebt: Boolean,
		hasFinancialDependednt: Boolean,
		hasStudentLoans: Boolean,
		income: Int)
	val financesDF = sc.textfile(...).toDF

Want to tally swiss students with debt and financial dependents. Spark DataFrame API can optimize the DAG, so don't ahve to worry about Order of Operations

	financesDF
		.join(filteredDemo, Seq("id"))
		.filter(
			col("country")==="Switzerland" &&
			col("hasDebt") &&
			col("hasFinancialDependent"))

### Optimizations
- Catalyst: Query Optimizer. Recall our earlier map of how Spark SQL relates to the rest of Spark. Catalyzes spark SQL down to a RDD
 - has full knowledge and understanding of all data types
 - knows the exact schema of our data
 - has detailed knowledge of the computations we'd like to do

Optimizing like:
 - reording operations
 - Reduce the amount of data we must read
 - Prunning unneeded partitioning

- Tungsten: off-heap serializer
 - highly-specialized data encoders
 - column based (can be serialized easily)
 - off heap (memory managed by Tunsten giving faster access outside heap -> free from garbage collection overhead)

Let's briefly develop some intuition about why structured data and computations enable these two backend compounds to do so many optimizations for you.
In summaray, lots of opportunaties to optimize operations on DataFrames opposed to aggressively optimizing RDDs.

### Limitations of DataFrames
Untyped `listingsDF. filter(col("state") === "CA")`, throws runtime exception.

Limited DataTypes: If your data cant be expressed by case classes/Products and standards Spark SQL data types,
may be difficult to ensure Tungsten encoder exists for your data type. E.G. you have an application which already uses some kind of complicated regular Scala class.

Requires SemiStructured/Structed Data
If your unstructured data cannot be reformulated to adhre to some kind of schema, would be better to use RDDs.

## Lecture 4.5 - Datasets
Let's say we've computed the average price of for sale per zipcode.

	case class Listing(street: String, zip: Int, price: Int)
	val listingsDF = ...
	import org.apache.spark.sql.functions._
	val averagePricesDF = listingsDF.groupBy("zip").avg("price")

calling collect will produce an Array[Row]

	val averagePrices = averagePriceDF.collect()
	
consult Row API docs

	averagePrices.head.schema.printTreeString()

So we can cast to double after seeing result time

	val averagePricesWorks = averagePrices.map{
		row => (row(0).asInstanceOf[Int], row(1).asInstanceOf[Double])
	}

Disgusting but works

DataFrames are actually Datasets ` type DataFrame = Dataset[Row]`. A dataset is
- distributed collection of data
- dataset API unifies the DataFrame and RDD APIs
- DataFrame require strucutred/ semi-structured data. Schemas and Encoders core part of Datasets

listingsDS is of type Dataset[Listing]

	listingsDS.groupByKey(l => l.zip)
		.agg(avg(col("price")).as[Double])

Datasets are something in between DataFrames and RDDs
- can still use relational DataFrame operations as learned in previous sessions on Datasets
- Datasets add more typed operations that can be used as well
- Datasets let you use higher order functions like map, flatMap, filter again

Creating Datasets: from a dataframe, use the `toDS` method (requires spark.implicits._). Can also read data from a JSON and convert to a dataset

	val myDS = spark.read.json("people.json").as[Person]

From an RDD, use the `toDS` convenience method `myRDD.toDS`. Remerb untyped transformations from DataFrames. DataSet API includes both untyped and typed transformations
- untyped transformations: transformation on DataFrames
- typed transformations typed variants of many DataFrames transformations + additional transformations such as RDD-like higher-order functions map, flatMap, etc.

These APIs are integrated. You can call a map on a DataFrame and get back a Dataset. Remember may have to explicity provide type information when going from a DataFrame
to a DataSet 

	val keyValuesDF = List((3, "Me"),(1,"Thi")).toDF
	val res = keyValuesDF.map(row => row(0).asInstanceOf[Int] + 1)

### Typed Transformations on DataSets
- map[U](f: T => U): Dataset[U]
- flatMap[U](f: T => TraversableOnce[U]): Dataset[U]
- filter(pred: T => Boolean): Dataset[T]
- distinct(): Dataset[T]
- groupByKey[K](f: T => K):KeyValueGroupDataSet[K, V]
- coalesce(numPartitions: Int): Dataset[T]
- repartition(numPartitions: Int): Dataset[T]

### Grouped Operations on DataSets
Like on DataFrames, Datasets have a special set of aggregation operations meant to be used after a call to groupByKey on a Dataset
- calling groupByKey on a Dataset returns a KeyValueGroupDataSet
- KeyValueGroupedDatasetcontains a number of aggregation operations which return Datasets

How to group and aggregate on Datasets?
1) Call groupByKey on a Dataset, get back a KeyValueGroupedDataset

2) use an aggregation operation on KeyValueGroupedDataset

- reduceGroups(f: (V, V) => V): Dataset[(K, V)]
- agg[U](col: TypedColumn[V, U]): Dataset[(K, V)], for TypedColumn, slect one of the operations available to col types, then pass through agg
- mapGroups[U](f: (K, Iterator[V]) => U): DataSet[U], applies the given function to each group of data. For each unique group, function will be pass the group key and 
iterator that contains all of the elements in teh group. The function can return an element of arbitrary type which will be returned as a new Dataset
- flatMapGroups[U](f: (K, Iterator[V]) => TraversableOnce[U]): Dataset[U], applies the given function to each group of dta, For each unique group, the function will be 
passed the group key and an iterator that contains all of the elements in teh group. The function can return an iterator containing elements of an arbitrary type which
will be returned as a new Dataset.

### reduceByKey
might notice that Datasets are missing an important transformation that we often used on RDDs: reduceByKey

**Challenge** Emulate the semantics of reduceByKey on a Dataset using Dataset operations presented so far. Assume we'd have 

	val keyValues = List((3, "Me"),(1,"Thi"),(2, "Se"),(3,"ssa"),(1, "sIsA"),(3,"ge:"),(3, "-)"),(2,"cre"),(2,"t")).toDF

Find a way to use Datasets to achieve the same result thta you would get if you put this data into an RDD `keyValuesRDD.reduceByKey(_+_)`

**Challenge** Emulate the semantics of reduceByKey on a Dataset using Dataset operations presented so far. Assume we'd have the following data set:

	val keyValuesDS = keyValues.toDS
	keyValuesDS
		.groupByKey(p => p._1)
		.mapGroups((k, vs) => (k, vs.foldLeft("")((acc, p) => acc + p._2)))
		.show()

	keyValuesDS
		.groupByKey(p => p._1)
		.mapValues(p => p._2)
		.reduceGroups((acc, str) => acc + str)

### Aggregators
A class that helps you generically aggregate data. Kind of like the aggregate method we saw RDDs

	import org.apache.spark.sql.expressions.Aggregator
	class Aggregator[-IN, BUF, OUT]

- In is the input type to the aggregator. When using an aggregator after groupByKey, this is the type that represents the value in the key/value pair
- BUF is the intermediate type during aggregation
- OUT is the type of the output of the aggregation

	val myAgg = new Aggregator[IN, BUF, OUT] {
		def zero: BUF = ...
		def reduce(b: BUF, a: IN): BUF = ...
		def merge(b1: BUF, b2: BUF): BUF = ...
		def finish(b: BUF): OUT = ...
	}.toColumn

	val strConcat = new Aggregator[(Int, String), String, String] {
		def zero: String = ""
		def reduce(b: String, a: (Int, String)): String = b + a._2
		def merge(b1: String, b2: String): String = b1 + b2
		def finish(b: String): String = r
	}.toColumn

Encoders are what converted your data between JVM objects and Spark SQls specialized interal (tabular) representation. They're required by all Datasets. Encoders are highly
specialized, optimized code generators that generate custom bytecode for serialization and deserialization of data. The serialized data is stored using Spark internal 
Tungsten binary format, allowing for operations on serialized data and improve memory utilization. What sets them apart from regular Java or Kryo serialization
- Limited to an optimal for primitives and case classes, Spark SQL data types, which are well-understood
- They contain schema information, which makes these highly optimized code generators possible, and enables optimization based on the shape of the data. Since Spark
understands the structure of data in Datasets, it can create more optimal layout in memory when caching Datasets
- Uses significantly less memory than Kryo/Java serialization
- >10x faster than Kryo serialization (Java serialization orders of magnitude slower)

Two ways to introduce encoders:
- Automatically (generally the case) via implicits from a SparkSession
- Explicitly, via org.apache.spark.sql.Encoder, which contains a large selection of methods for creating Encoders from Scala primitive types and Products

Some examples of 'Encoder' creation methods in 'Encoders':
- INT/LONG/STRING etc, for nullable primitives
- saclaInt/ScalaLong/ScalaByte etc, for Scala's primitives
- product/tuple for Scala's Product and tuple types

Example: Explicitly creating Encoders

	val strConcat = new Aggregator[(Int, String), String, String] {
		def zero: String = ""
		def reduce(b: String, a: (Int, String)): String = b + a._2
		def merge(b1: String, b2: String): String = b1 + b2
		def finish(b: String): String = r
		override def buffEncoder: Encoder[String] = Encoder.STRING
		override def outputEncoder: Encoder[String] = Encoder.STRING
	}.toColumn

	keyValuesDS
	.groupByKey(pair => pair._1)
	.agg(strConcat.as[String])

### When to use Datasets vs DataFrames vs RDDs
Use Datasets when
- you have structured/semistructured data
- want typesafety
- you need to work with functional APIs
- you need good performance, but it doesn't have to be the best

Use DataFrames when
- you have structured/semistructured data
- you want the best possible eprformance, sutomatically ioptimized for you

Use RDDs when 
- you have unstructured data
- you need to fine-tune and manage low-level details of RDD computations
- you have complex data types that cannot be serialized with Encoders

### Limitations of Datasets
Catalyst Can't Optimize All Operations: 
For example, take `filter`

**Relational filter opertation** E.g, ds.filter(col("city").as[String] === "Boston")
Performance best because you're explicitly telling Spark which columns/attributes and conditions are required in your filter operation. With information about the structure
of the data and the structure of computations, Spark optimizer knows it can access only the fiedls involved in the filter without having to instantiate the entire data 
type. Avoids data moving over the network. 
*Catalyst optimizes this case*

**Functional filter operation** E.g. ds.filter(p => p.city == "Boston")
Same filter written with a function literal is opaque to Spark- it's impossible for Spark to introspect the lambda function. All Spark knows is that you need a (whole) 
record marshaled as a Scala object in order to return true or false, requiring Spark to do porentially a lot more work to meet that implicit requirement.
*Catalyst cannot optimize this case*

**Catalyst Can't Optimize All Operations** Takeaways:
- when using Datasets with higher-order functions like map, you miss out on many Catalyst optimizations
- When using Datasets with relational operations like select, you get all of Catalyst optimizations
- Though not all operations on Datasets beenfit from Catalyst's optimizations, Tungsten is still always running under the hood of Datasets storing and organizing data
in a highly optimized way, which can result in large speedups over RDDs

**Limited Data Types**
If your data can't be expressed by case classes/Products and standard Spark SQL data types, it may be difficult to ensure that a Tungsten encoder exists for your data type

i.e. you have an application which already uses some kind of complicated regular Scala class.

**Requires Semi-Structured/Structured Data**
If your unstructured data cannot be reformulated to adhere to some kind of schema, it would be better to use RDDs.
