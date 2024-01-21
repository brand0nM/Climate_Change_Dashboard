# Functions and State
Introducing the combination of mutable states and higher order functions

## Lecture 4.1 - Functions and State
Before programs were free of "side effects" or the concept of *time* wasn't important. For all programs 
that terminate, any sequence of actions would have given the same results. Was also reflected in the
substitution model.

### Reminder: Substitution Model
Programs can be evaluated by rewriting. The most important rewrite rule covers function applications
Abstractly

	def f(x1, ..., xn) = B; ...f(v1, ..., vn)
	def f(x1, ..., xn) = B; ...f(v1/x1, ..., vn/xn)B

#### Example
Say you have the following two functions `iterate` and `square`

	def iterate(n: Int, f: Int => Int, x: Int) =
		if n == 0 then x else iterate(n-1, f, f(x))
	def square(x: int) = x*x
	iterate(1, square, 3)
	> if 1 == 0 then 3 else iterarte(1-1, square, square(3))
	> iterate(0, square, square(3))
	> iterate(0, square, 3*3)
	> iterate(0, square, 9)
	> if 0 == 0 then 9 else iterate(0-1, square, square(9))
	> 9

Rewriting can be done anywhere in a term, and all rewriting which terminate lead to the same solution

!()["pictures/kurt_russle_theorem.png"]

### Stateful Objects
One normally describes the world as a set of objects, some of which have state that 
changes over the course of time.

An object as a state if its behavior is influenced by its history.

*Example:* a bank account has a state, because the answer to the question "can I withdraw 100 CHF?"
may vary over the course of the lifetime of the account.

### Implementation for State
Every form of mutable state is constructed from variables. Similar to variable definition but uses `var`. 
Key distincting is that variables can be rewritten, i.e. `var x = 1; x = x+2`.

### State in Objects
In practice, objects with state are usually represented by objects that have some variable members. 
For instance, here is a class modeling a bank account

	class BankAccount:
		private var balance = 0

		def deposit(amount: Int): Unit =
			if amount >0 then balance = balance + amount
		def withdraw(amount: Int): Int =
			if 0<amount && amount <= balance then
				balance = balance - amount 
				balance
			else throw Error("insufficient funds")

So the BankAccount class defines the current state of an account where the deposit and withdraw 
methods change the vbalance through adssignments.
Not the balance is private and cannot be seen or accessed outside class.

	val account = BankAccount()

### Statefulnesss and Variables
Recall the mplememntation of `TailLazyList` instead of using a lazy val, could also use non-empty lazy 
lists with mutable variables.

	def cons[T](hd: T, tl: TasilLazyList) = new TailLazyList[T]:
		def head = hd
		private var tlOpt: Option[TailLazyList[T]] = None
		def tail: T = tlOpt match
			case Some(x) => x
			case None => tlOpt = Some(tl); tail

Is the result of cons stateful? depends on if the rest of program is purely functional. In purely functional 
world, there is no point to compute tail each instance, so one should used the cashed value that will not 
change any obervations made on this TailLazyList. In this instance, 
`val xsw = cons(1, {primative("!"), Empty})`
the environment has side effects, then tail optimization with mutable states would fail 
`xs.tail // !; xs.tail //`.

	class BankAccountProxy(ba: BankAccount):
		def deposit(amount: Int): Unit = ba.deposit(amount)
		def withdraw(amount: Int): Int = ba.withdraw(amount)

Are these instances of `BankAccountProxy` stateful objects? Yes since the parameter bank account is stateful 
and hold a mutable history

## Lecture 4.2 - Identity and Change
Assignment poses the new problem of deciding whether two expressions are the same.
`val x = E; val y = E` should be the same as `val x = E; val y = x`, otherwise known as referential 
transparency. 

	val x = BankAccount()
	val y = BankAccount()

no, since two different instantiations of same class, but yes because they arte both empty bank accounts

### Operational Equivalence
To respond, must specify what is meant by "the same". Precise emaning of "being the same" is defined by 
the property of **operational equivalence**. Property says, suppose we have 2 definitions x and y. x and y are 
operationally equivalent if no possible test can distinguish between them.

	val x = BankAccount()
	val y = BankAccount()

define values x and y that are the same. Follow definitions.. bad proof use `hashCode` to understand

### Assignment and Substitution Model
One can alwaysy replace the name of a vlaue by the expression that defines it. *example:*

	val x = BankAccount()
	val y = x
But thiis leads to a different program (since val is not mutable), making the substitution model cease. 
Possible to adapt the substitution model using s **store** (tracks all your variable bindings), 
but this becomes more complicated.

## Lecture 4.3 - Loops
**Proposition** variables are enough to model all imperative programs.
But what about control statements like loops?
We can model them using functions

*Example:* Here is a Scala program that uses a while loop:

	def power(x: Double, exp: Int): Double =
		var r = 1.0
		var i = exp
		while i>0 do {r = r*x; i = i-1}
		r

`while-do` loops are a built-in contruct

Could define it using a function

	def whileDo(condition: => Boolean)(command: => Unit): Unit =
		if condition then
			command
			whileDo(condition)(command)
		else ()

Notes:
- condition and command must be passed by name so they're reevaluated in each iter
- whileDo is tail recursive, so it can operate with a constant stack size

#### Exercise
1) Write a function implementing a `repeat` loop that is used as follows

	repeatUnit{
		command
	}(condition)

Should execute command 1 or more times until condition is true. Similarly

	def repeatUnit(condition: => Boolean)(command: => Unit): Unit =
		command
		if !condition then repeatUnit(condition)(command)

2)

	repeat{
		command
	}.until(condition)

	def repeat(body: => Unit) = Until(body)
	class Until(body: => Unit) with
		infix def until(cond: => Boolean): Unit =
			if !cond then
				body
				until(cond)

### For-Loops
the classical `for` loop can not be implemented easily in higher-order functions. 
In Java `for (int i =1; i<3; i=i+1){System.out.print(i+" "); }`, the asrguments of `for` contain the 
declaration of the variable `i` which is visible in other arguments and in the body.

In Scala, `for i<- 1 until 3 do System.out.print(s"$i ")`

### translation of For-Loops
translate similarly to for-expressions, but using the foreach combinor instead of `map` and `flatmap`.

`foreach` is defined on collections with elements of type T as follows:

	def foreach(f: T => Unit): Unit = // apply 'f' to each element of the collection

Example

	for i <- 1 until 3; j <- "abc" do println(s"$j $i")

translates to

	(1 until 3).foreach(i => "abc".foreach(j => println(s"$j $i"))

## Lecture 4.4 - Extended Example: Discrete Event Simulation
**Advanced example**, consider how assignment and higher order functions can be combined in interesting ways. Construct a digital circuit simulator. Shows how to build programs
that do discrete event simulation.

### Digital Ciruit
A digital ciruit has 2 parts
- wires that transport signals: which are transformed by componenets (represented by Booleans)
- base components (gates)
 - The inverter, whose output is the inverse of tis input
 - the and gate, whiose output is the conjjustion of its inputs
 - the or gate, whose output is the disjunction of its inputs
 
Other components can be constructed by combining thser base components. These components have a reaction time (or delay), and their outputs don't change immediately after a change in 
their inputs. 

#### Diagrams
half-adder: for carry to be set, both a and b must be set. for sum to be set, either a, or b have to be set, then carry must be false to produce a sum

Once defined can use 2 half adders to define a full adder
!()["pictures/circuits.png"]

#### Code implementation
have a circuit board, add componets as actions with sideeffects on model. To start need class wire

	val a, b, c = Wire()

Then need functions to create base components as a side effect

	def inverter(input: Wire, output: Wire): Unit
	def andGate(in1: Wire, in2: Wire, output: Wire): Unit
	def orGate(in1: Wire, in2: Wire, output: Wire): Unit

Constructing components

	def halfAdder(a: Wire, b: Wire, s: Wire, c: Wire): Unit = 
		val d = Wire()
		val c = Wire
		orGate(a, b, d)
		andGate(a, b, c)
		inverter(c, e)
		andGate(d, e, s)

!()["pictures/coded_ciruit.png"]

	def fullAdder(a: Wire, b: Wire, cin: Wire, sum: Wire, cout: Wire): Unit =
		val s = Wire()
		val c1 = Wire()
		val c2 = Wire()
		halfAdder(a, cin, s, c1)
		halfAdder(b, s, sum, c2)
		orGater(c1, c2, cout)

#### Exercise
What logical function does this program describe

	def f(a: Wire, b: Wire, c: Wire): unit =
		val d, e, f, g = Wire()
		inverter(a, d)
		inverter(b, e)
		andGate(a, e, f)
		andGate(b, d, g)
		orGate(f, g, c)

	a != b

### Implementation
The class wire and the functions inverter, andGate, orGate represent a small description language of digital ciruits. based on simple API for discrete event simulation. 

**Actions** disrcete event simulator performs *actions*, specified by the user at a given *moment*. An *action* is a function that doesnt take any parameters anbd which returns `Unit`

	type Action = () => Unit

The *time* is simulated; it has nothing to do with the actual time. 

**Simulation Trait** concrete siml;uation happens inside an object that inherits form the abstract class `Simulation`, which has the following signature:

	trait Simulation:
		def currentTime: Int = ???
		def afterDelay(delay: int)(block => Unit): Unit = ???
		def run(): Unit = ???

Here `currentTime` returns the current simulated time in the form of an integer.

`afterDelay` registers an action to perform after a certain delay (relative to the current time).

`run` performs the simlation until there are no more actions.

*Class Diagram:* Simulation => Gates => Circuits => Sim (concrete simulation)

### Gates
**The wire Class:** must support three operations `getSignal(): Boolean`, return the current value of the signal transported by the wire `setSignal(sig: Boolean): Unit`, modify the value 
of the signal transported by the wire `addAction(a: Action): Unit` 

Attaches the specified procedures of the actions of the wire. All of the attached actions are executed at each change of the signal

#### Implementing Wires
Here is an implementation of class Wire

	class Wires:
		private var sigVal= false
		private var actions: List[Action] = List()

		def getSignal(): Boolean = sigVal

		def setSignal(s: Boolean): Unit =
			if s != sigVal then
				sigVal = s
				actions.foreach(_()) // for a <- actions do a()

		def addAction(a: action): Unit =
			actions = a :: actions
			a()

#### Inverter
implement by installing an action on its input wire. Change must be efgfective after a delay of InverterDelay units of time

	def inverter(input: Wire, output: Wire): Unit =
		def invertAction(): Unit =
			val inputSig = input.getSignal()
			afterDelay(InverterDelay){ output.setSignal(!inputSig) }
		input.addAction(invertAction)

#### And Gate

	def andGate(in1: Wire, in2: Wire, output: Wire): Unit =
		def andAction(): Unit =
			val inputSig1 = in1.getSignal()
			val inputSig1 = in2.getSignal()
			afterDelay(InverterDelay){ output.setSignal(inputSig1 & inputSig2) }
		inputSig1.addAction(andAction)
		inputSig2.addAction(andAction)

#### Or Gate

	def orGate(in1: Wire, in2: Wire, output: Wire): Unit =
		def orAction(): Unit =
			val inputSig1 = in1.getSignal()
			val inputSig1 = in2.getSignal()
			afterDelay(InverterDelay){ output.setSignal(inputSig1 | inputSig2) }
		inputSig1.addAction(orAction)
		inputSig2.addAction(orAction)

Why can't we do 

	def orGate(in1: Wire, in2: Wire, output: Wire): Unit =
		def orAction(): Unit =
			afterDelay(InverterDelay){ output.setSignal(in1.getSignal() | in2.getSignal()) }
		inputSig1.addAction(orAction)
		inputSig2.addAction(orAction)


- does not model Or gates faithfully because they do not account for time delay

### Simulation Trait
We have left to do is implement the `Simulation` trait. idea is to keep in every instance of the `Simulation` *agenbda* of actions to perform. Agenda is a paur, each ocmposed of an action and
the time when it must be produced. Agenda list is sorted in such a way that the actions to be performed first are at the beginning.

	trait Simulation:
		private type Action = () => Unit
		private case class Event(time: int, action: Action)
		private type Agenda = List[Event]
		private var agenda: Agenda = List()
		private var curtime = 0

Application of the `afterDelay(delay)(block)` method inserts the task `Event(curtime + delay, () => block)`, into the agenda list at the right position.

	def afterDelay(delay: int)(block: => Unit): Unit = 
		val item = Event(currentTime + delay, () => block)
		agenda = insert(agenda, item)

	private def insert(ag: list[Event], item: Event): List[Event] = ag match
		case first :: rest if first.time <= item.time =>
			first :: insert(rest, item)
		case _ => item :: ag

Event Handling Loop, removes successive elements from the agenda and performs the associated actions

	private def loop(): Unit = agenda match
		case first :: rest =>
			agenda = rest
			curtime = first.time
			first.action()
			loop()
		case Nil => 

Run implementaion, removes successive elements from agenda list, and performs associated actions

	def run(): Unit =
		afterDelay(0) {
			println(s"***simulation started, time = $currentTime ***")
		}
		loop()

?Does every simulation terminate after a finite amount of steps? No, consider a simulation where each truth value of an actions leads to the call of a new actions, where the action is the 
same- repeating simulation- i.e. loops to one or more calculations.

Probes, before launching simulation, we want a way top debug/examine changes iof the signals on the wire. 

	def probe(name: String, Wire): Unit =
		def probAction(): Unit =
			println(s"$name $currentTime value = ${wire.getSignal()}"}
		wire.addAction(probeAction)

### Putting it together

	trait Simulation:
		private type Action = () => Unit

		private case class Event(time: int, action: Action)
		private type Agenda = List[Event]
		private var agenda: Agenda = List()
		private var curtime = 0

		def currentTime: Int = curtime

	def afterDelay(delay: int)(block: => Unit): Unit = 
		val item = Event(currentTime + delay, () => block)
		agenda = insert(agenda, item)

	def run(): Unit =
		afterDelay(0) {
			println(s"***simulation started, time = $currentTime ***")
		}
		loop()

	private def loop(): Unit = agenda match
		case first :: rest =>
			agenda = rest
			curtime = first.time
			first.action()
			loop()
		case Nil => 

	trait Gates extends Simulation:

		def InverterDelay: Int
		def andGateDelay: Int
		def orGateDelay: Int

		class Wires:
			private var sigVal= false
			private var actions: List[Action] = List()

			def getSignal(): Boolean = sigVal

			def setSignal(s: Boolean): Unit =
				if s != sigVal then
					sigVal = s
					actions.foreach(_()) // for a <- actions do a()

			def addAction(a: action): Unit =
				actions = a :: actions
				a()

		def inverter(input: Wire, output: Wire): Unit =
			def invertAction(): Unit =
				val inputSig = input.getSignal()
				afterDelay(InverterDelay){ output.setSignal(!inputSig) }
			input.addAction(invertAction)
		def andGate(in1: Wire, in2: Wire, output: Wire): Unit =
			def andAction(): Unit =
				val inputSig1 = in1.getSignal()
				val inputSig1 = in2.getSignal()
				afterDelay(InverterDelay){ output.setSignal(inputSig1 & inputSig2) }
			inputSig1.addAction(andAction)
			inputSig2.addAction(andAction)
		def orGate(in1: Wire, in2: Wire, output: Wire): Unit =
			def orAction(): Unit =
				val inputSig1 = in1.getSignal()
				val inputSig1 = in2.getSignal()
				afterDelay(InverterDelay){ output.setSignal(inputSig1 | inputSig2) }
			inputSig1.addAction(orAction)
			inputSig2.addAction(orAction)
		def probe(name: String, Wire): Unit =
			def probAction(): Unit =
				println(s"$name $currentTime value = ${wire.getSignal()}"}
			wire.addAction(probeAction)

	trait Circuits extends Gates:

		def halfAdder(a: Wire, b: Wire, s: Wire, c: Wire): Unit = 
			val d = Wire()
			val c = Wire
			orGate(a, b, d)
			andGate(a, b, c)
			inverter(c, e)
			andGate(d, e, s)

		def fullAdder(a: Wire, b: Wire, cin: Wire, sum: Wire, cout: Wire): Unit =
			val s = Wire()
			val c1 = Wire()
			val c2 = Wire()
			halfAdder(a, cin, s, c1)
			halfAdder(b, s, sum, c2)
			orGater(c1, c2, cout)

	object sim extends Circuits, Delays
	
	trait Delays:
		def InverterDelay = 2
		def andGateDelay = 3
		def orGateDelay = 5

#### Setting Up a Simulation
define four wires and place some probes

	import sim.*
	val input1, input2, sum, carry = Wire()
	probe("sum", sum)
	probe("carry", carry)

Next define a half-adder using these wires

	halfAdder(input1, input2, sum, carry)

#### Launching Simulation

	object sim extends Circuits, Delays
	
	trait Delays:
		def InverterDelay = 2
		def andGateDelay = 3
		def orGateDelay = 5

	import sim.*
	val input1, input2, sum, carry = Wire()
	probe("sum", sum) // sum 0 value = false
	probe("carry", carry) // carry 0 value = false

	halfAdder(input1, input2, sum, carry)

	input1.setSingal(true)
	run() // ** simulation started, time 0
	// sum 8 value = true

	input2.setSignal(true)
	run() // ** simulation started, time 8
	// carry 11 value = true
	// sum 16 value = false

### Alternate orDef
Can be defined in terms of inverse and 

	def orGateAlt(in1: Wire, in2: Wire, output: Wire): Unit =
		val notIn1, notOut = Wire()
		inverter(in1, notIn1); inverter(in2, notIn2)
		andGate(notIn1, notIn2, notOut)
		inverter(notOut, output)

This simulation will result in different simulated times, and may produce additional events

### Summary
States and assignments make the model more complicated. Also loose referential transparency, as variables are constanly changing.
On the other hand, assignments allow us to formulatre certyain programs in an elegant way

EX: discrete event simulation
- here a system is represented by a mutable list of actions
- the effect of actions, when they're called change the state of objects and can also install other actions to be executed in the future.






























































































































