package calculator

// Trait Signal for recording the State of objects
trait Signal[+T]:
  def apply()(using caller: Signal.Caller): T
  def currentValue: T

// Instantiation of Signal
object Signal:
  /** Abstract Subclass for extending Signal; Keeps states updated by
  * creating a new instantiation for any changes that occur to the Signal */
  abstract class AbstractSignal[+T] extends Signal[T]:
    // Declare a base Abstract Signals' value
    private var _currentValue: T = compiletime.uninitialized
    // Initialize an Empty collection of AbstractSignals
    private var observers: Set[Caller] = Set()

    // Instantiates how to map an Abstract Signal to type T
    protected def eval: Caller => T 
    // Method for decoupling Dependent AbstractSignals
    protected def computeValue(): Unit =
      val newValue = eval(this) // Map this Abstract Signal to type T
      // Boolean to track any changes in our Abstract Signal
      val observeChange = observers.nonEmpty && newValue != _currentValue
      _currentValue = newValue // Assign considered value to our current value
      if observeChange then
        val obs = observers // Copy Abstract Signals dependent on considered Signal
        observers = Set() // Empty observers of the current Abstract Signal
        obs.foreach(_.computeValue()) // Recursively decouple dependent Abstract Signals

    // Add Caller to our Set, but make sure this Abstract Signal is not a member
    def apply()(using caller: Caller): T =
      observers += caller
      assert(!caller.observers.contains(this), "cyclic signal definition")
      _currentValue

    // After the Abstract Signal has been constructed define its current value
    def currentValue: T = _currentValue
  end AbstractSignal

  /** To keep states linked, want to create a new instantiation of Abstract signal
  * every time changes occure */
  def apply[T](expr: Caller ?=> T): Signal[T] =
    new AbstractSignal[T]:
      protected val eval = expr(using _)
      computeValue()

  @annotation.implicitNotFound(
    """You can only observe a Signal value within a Signal definition like in `Signal{ ... }`. 
    If you want to just read the current value, use the method `currentValue`."""
  )
  // Data Structure left undefined for 
  opaque type Caller = AbstractSignal[?]

  // If Caller type is different create a method to modify our Abtsract Signal
  class Var[T](expr: Signal.Caller ?=> T) extends Signal.AbstractSignal[T]:
    protected var eval: Signal.Caller => T = expr(using _)
    computeValue()

    // Method to Modify this Caller
    def update(expr: Signal.Caller ?=> T): Unit =
      eval = expr(using _)
      computeValue()
  end Var

end Signal
