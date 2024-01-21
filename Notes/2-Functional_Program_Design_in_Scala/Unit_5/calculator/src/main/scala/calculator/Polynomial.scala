package calculator

// Instantiation of Polynomial 
object Polynomial extends PolynomialInterface:
  // Dpdated Signal with modified polynomial values
  def computeDelta(a: Signal[Double], b: Signal[Double],
      c: Signal[Double]): Signal[Double] = 
  	Signal((b()*b())-4*(a()*c())) 
  def computeSolutions(a: Signal[Double], b: Signal[Double],
  	c: Signal[Double], delta: Signal[Double]): Signal[Set[Double]] = 
  	Signal(
  		delta() match {
        // Returns Roots if real
  			case posDelta if posDelta>=0 => { 
  				Set((-b()-math.sqrt((delta())))/(2*a()),
  					(-b()+math.sqrt(delta()))/(2*a()))}
  			// Returns empty if roots are imaginary
  			case _ => Set.empty
  		}
  	)