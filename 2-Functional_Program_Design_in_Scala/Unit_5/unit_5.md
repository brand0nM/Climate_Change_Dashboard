# Timely Effects
Functional reactive programs allows use to express in a purley functional way time varying signals: as opposed to imperative state changes through variables.
Our implementation of functional reactive programming also uses internal states, but the state is not encloesd and does not impare the reasoning one can do 
with a functional surface language.

## Lecture 5.1 - Imperative Event Handling: The Observer Pattern
### Observer Pattern
widely used when views need to react to changes in a model. Variants are also called publish/subscribe and model/view/controller (MVC)

Idea, when their are updates to a model, publishes will notify subscriber of updates through views.

### Publisher and Subscriber Traits

	trait Subscriber:
		def handler(pub: Publisher): Unit

	trait Publisher:
		private var subscribers: Set[Subscriber] = Set()
		
		def subscribe(subscriber: Subscriber): Unit =
			subscribers += subscriber
		def unsubscribe(subscriber: Subscriber): Unit =
			subscribers -= subscriber
		def publish(): Unit =
			subscribers.foreach(_.handler(this))

### Observing Bank Account

	class BankAccount extends Publisher:
		private var balance = 0
		def currentBalance: int = balance
		def deposite(amount: int): Unit =
			if amount>0 then 
				balance = balance + amount
				publish()	
		def withdraw(amount: int): Unit =
			if 0<amount && amount <= balance then
				balance = balance - amount
				publish()
			else throw Error("insufficient funds")

#### An observer
A `Subscriber` to maintain the total balance of a list of accounts:

	class Consolidator(observed: List[BankAccount]) extends Subscriber:
		observed.foreach(_.subscriber(this))

		private var total: Int = _ // dont initialize total yet, but type variable
		private def compute() =
			total = observed.map(_.currentBalance).sum	

		compute() // assigned in compute

		def handler(pub: Publisher) = compute()
		def totalBalance = total

The positive
- decouples views from state
- allows to have varying number of views of a given state
- simple to set up

The downsides
- Forces an imperative type since the handlers are Unit typed
- Many moving parts that need to be co-ordinated
- Concurrency makes things more complicated
- Views are tightly bound to one state; view update happens immediately

Idea quantified
- 1/3 of adobe desktop applications are devoted to event handling
- 1/2 of the bugs are found in this code

### How to impove
Explore functional reactive programming which improves on reactive programming

## Lecture 5.2 - Functional Reactive Programming
### Functional reactive programming (FRP)
*def:* is about reacting to sequences of events that happen in time. Functional views aggregate an event sequence into a signal
- A signal is a value that changes over time
- it is represented as a function from time to the value domain
- instead of propagating updates to mutable states, we define new signals in terms of existing ones

#### Example
Whenever the mouse moevs, an event `MouseMoved(toPos: Position)` if fired.
**FRP view:** A singal, `mousePosition: Signal[Position]` which at any point in time represents the current mouse position

### Origins of paradigm
FRP started in 97 with the paper *Functional Reactive Animation*. Many FRP systems since both standalone languages and embedded 
libraries. *Flapjax*, *Elm*, *react.js*. Event streaming dataflow programming systems such as RX are related but the term FRP is 
not commonly used for them. 

We'll introduce FRP by means of a minimal class `frp.Signal` whose implementation is explained at the 
end of model; frp signal is modelled after Scala.react, which is described in the paper *Deprecating the Observer Pattern*

### Fundemental Signal Operations
There are two fundemental operations over signals:

1) Obtain the value of the signal at the current time. In our library this is expressed by () application `mousePosition()`

2) Define a signal in terms of other signals. In our library, this is expressed by the Signal constructor

	def inRectangle (LL: Postion, UR: Position): Boolean =
		Signal {
			val pos = mousePosition()
			LL <= pos && pos <= UR
		}

	val b = inRectangle(L, R)

with

	def inRectangle (LL: Postion, UR: Position): Signal[Boolean] =
		Signal {
			val pos = mousePosition()
			LL <= pos && pos <= UR
		}

	val s = inRectangle(L, R)
	s() // result in state1
	s() // results in state2 != state1 if something has changed

`inRectangle` creates a signal that, *at any point in time* is equal to the test whether mousePosition *at that point in time* is in
the box [LL..UR].

**Constant Signal:** The `Signal(...)` syntax can also be used to define a signal that has always the same value: `val sig = Signal(3)`

### Computing Signals
The idea of FRP is quite general. It does not prescribe whether signals are continuous or discrete and how a signal is evaluated.

There are several posibilities

1) A signal could be evaluated on demand every time its value is needed

2) A continuous signal could be sampled at certain points and interpolated in-between

3) Updates to discrete signal could be propagated automatically to dependent signals.

The last possibility is the fubnctional analogue to the observer pattern. We will purse this one in the rest of this unit.

### Time-Varying Signals
How do we define a signal that varies in time
- we can  uuse externally defined signals, such as `mousePosition` and map over them
- Or we can use a `Signal.Var`

### Variable Signals
Expressions of type `Signal`1 cannot be updated. But our library also defines a subclass `Signal.var` of `Signal` for signals that can be changed. `Signal.Var` provides an "update" operation, which allows to redfine the value of a asignal from the current time on.

	val sig = Signal.Var(3) 
	sig.update(5)

#### Update Syntax
In Scala, calls tro pupdate can be written as assignments. For instance, for an array `arr`, `arr(i) = 0` is translated to 
`arr.update(i,0)`. This calls an update method which can be thought of as follows:

	class Array[T]:
		def update(idx: Int, value: T): Unit
		...

Generally the assignment likw `f(E1, ..., En) = E` is translated to `f.update(E1, ..., En, E)`. Thi sworksalso if n= (): f() = E is 
shorthand for `f.update(E)` Hence `sig.update(5)` can be abrevaited to sig() = 5

### Signals and Variables
Signals of type `Signal.Var` look a bit like mutable variables, where `sig()` is deferencing, ann `sig() = newValue` is update. But 
there is a crucial difference: We can apply fucntions to signals that is maintained automatically , at all future pointas in time. 
No such mechanism exists for mutable variables; we have to propagate all updates manually.

EX1: `var x = 1; var y = x*3; x = 3` and EX2: `val x = Signal.Var(1); val y = Signal.Var(x*3); x() = 3`. 
For EX1, y would be equal to 3, since atr y's delaration it was mutable, but equal to 1*3. 
EX2 would be equal to 9 , since the state of y is dependent of the variable x times 3.

#### Example
Repeat the BankAccount example of the last section with signals. Add signal valance to `BankAccounts`. Define a function `consolidated`
which produces the sum of all balances of a given list of accounts. What savings were possible compared to the publish/subscribe 
implementations.

	import frp._

	class BankAccount:
		def balance: Signal[Int] = myBalance
		private var myBalance = Signal[Int](0)

		def deposite(amount: int): Unit =
			if amount>0 then
				val b = myBalance()
				myBalance() =  b + amount

		def withdraw(amount: int): Unit =
			if 0<amount && amount <= balance() then
				val b = myBalance()
				myBalance = b - amount
				myBalance()
			else assertFail("insufficient funds")

	def consolidated(accts: List[BankAccount]): Signal[Int] =
		Signal(accts.map(_.balance()).sum)

	val a = BankAccount()
	val b = BankAccount()
	val c = consolidated(List(a, b))
	c()

	a.deposite(10)

Consider the two code fragments below

	1) val num = Signal.var(1)
	val twice = Signal(num()*2)
	num() = 2
	2) var num = Signal.Var(1)
	val twice = Signal(num()*2)
	num = Signal.var(2)

Do they yield the same final value for twice()? 
no, since the first example has linked states; `twice() = 4`
and the second example has a value dependent on the variable definition of num

## Lecture 5.3 - A Simple FRP Implementation
### A simple FRP Implememntation
We now develop a simple implementation of `Signal` and `Vars`, which together make up the basis of our approach to functional reactive
programming. The classes are assumed to be in a package `frp`. 

### Summary: Signal and Var API

	trait Signal[+T]:
		def apply(): T = ???

	object Signal:
		def apply[T](expr: => T) = ???

		class Var[T](expr: => T) extends Signal[T]
			def update(expr: => T): Unit = ???

note that `Signals` are covariant, but `Vars` are not

### Implementation Idea
Each signal maintais
- its current value
- theecurrent exopression that defines the signal value
- a set `oservers:` the other signal that depends on its value

Then, if the signal changes, all observers need to be re-evaluated

### Dependency Maintenance
 How do we record dependencies in obervers?
- When evaluating a signal- value expression, need to know which signal caller gets defined or updated by the expression
- if we know that, then executing a sig() meanbs adding caller to the observers of sig
- When signal sig's value changes, all  peviously observing signals are re-evaluated and the set sig.observer is cleared
- Re-evaluation will re-enter a calling signal caller in sig.observers, as long as caller's value still depends on sig
- For the moment, let's assume that caller is provided "magically"; we will discuss later how to make that happen

### Implementation of Signals
The `Signal` trait is implemented by a class `AbstractSignal` in the `Signal` object. This is a useful and common implementation
technique that allows us to hide glocal implementation details in the enclosing object.

	object Signal:
		opaque type Observer = AbstractSignal[?]
		def caller: Observer = ???

		abstract class AbstractSignal[+T] extends Signal[T]:
			private var currentValue: T = _
			private var observers: Set[observer] = Set()

		def apply(): T =
			observer += caller
			currentValue

		protected def eval: () => T

### Re-Evaluating Signal Values
A signal alue is evaluated using `computeValue()`
- on initialization
- when an observed signal changes its value

Here is its implementation

	protected def computeValue(): Unit =
		val newValue = eval()
		val observeChange = observers.nonEmpty && newValue != currentValue
		currentValue = newValue
		if observeChange then
			val obs = observers
			observers = Set()
			obs.foreach(_.computeValue())

### Creating Signals
Signals are created with an `apply` method in the `Signal` object

	object Signal:
		def apply[T](expr: => T): Signal[T] =
			new AbstractSignal[T]:
				val eval = () => expr
				computeValue()

### Signal Variables
The `Signal` object also defines a class for variable signals with an update method

	class Var[T](initExpr: => T) extends AbstractSignal[T]:
		protected var eval = () => initExpr
		computeValue()

		def update(newExpr: => T): Unit =
			eval = () => newExpr
			computeValue()

### Who's Caller
need a higher order function that instead of mapping expr: => T, creates a map from one Observer instanbce to a type.
When evaluating a signal, s() becomes s(caller). Problem This causes a lot of boilerplate code, and it's easy to get wrong!

#### Calling, Implicitly
How about we make signal evaulation expressions implicit function types? ` expr: Observer ?=> T`. Then all caller parameters are passed
implicitly In the following we will use the type alias `type Observed[T] = Observer ?=> T`.

#### New Signal and Var APIs

	trait Signal[+T]:
		def apply(): Signal.Observed[T]

	object signal:
		def apply[T](expr: Observed[T]): Signal[T] = ???

		class Var[T](expr: Observed[T]) extends AbstractSignal[T]
			def update(expr: Observed[T]): Unit = ???

### Putting it Together

	object frp:

		trait Signal[+T]:
			def apply(): T

		object Signal:

			opaque type Observer = AbstractSignal[?]
			type Observed[T] = Observer ?=> T
			def caller(using o: Observer) = o

			abstract class AbstractSignal[+T] extends Signal[T]:
				private var currentValue: T = _
				private var observers: SetObserver] = Set()
				currentValue

				protected def eval: () => Observed[T]

				protected def computeValue(): Unit =
					val newValue = eval(using this)
					val observeChange = observers.nonEmpty && newValue != currentValue
					currentValue = newValue
					if observeChange then
						val obs = observers
						observers = Set()
						obs.foreach(_.computeValue())

			def apply(): Observed[T] =
				observers += caller
				assert(!caller.observers.contains(this), "cyclic signal defintion")
				currentValue

		def apply[T](expr: Observed[T]): Signal[T] = new AbstactSignal[T]:
			val eval = expr
			computeValue()

		class Var[T](expr: Observed[T]) extends AbstractSignal[T]:
			protected var eval = expr
			computeValue()

			def update(expr: Observed[T]): Unit =
				eval = expr
				computeValue()

### Evaulating Signals from he Outside
At the root, it's application that evaluates a signal. So there's no other signal that is the "caller."
To deal with this situation, we define a special given instance noObserver in Signal.

	given noObserver: Observer = new AbstractSignal[Nothing]:
		override def eval = ???
		override def computeValue() = ()

Since `noObserver` is the compaanion object of `signal` it's a;ways applicable when an Observer is needed. but inside Signl {...} 
expressions, it's the implicitly provided observer that takes precedence since it is in the lexically enclosing scope.

	class BankAccount:
		def balance: Signal[Int] = myBalance

		private var myBalance = Signal.Var[Int](0)

		def deposite(amount: int): Unit =
			if amount>0 then
				val b = myBalance()
				myBalance() =  b + amount

		def withdraw(amount: int): Unit =
			if 0<amount && amount <= balance() then
				val b = myBalance()
				myBalance = b - amount
				myBalance()
			else assertFail("insufficient funds")

	def consolidated(accts: List[BankAccount]): Signal[Int] =
		Signal(accts.map(_.balance()).sum)

	val a = BankAccount()
	val b = BankAccount()
	val c = consolidated(List(a, b))
	c()
































