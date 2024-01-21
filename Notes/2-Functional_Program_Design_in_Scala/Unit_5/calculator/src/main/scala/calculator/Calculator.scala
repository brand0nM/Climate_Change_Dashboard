package calculator

// Cases of an expression
enum Expr:
  case Literal(v: Double)
  case Ref(name: String)
  case Plus(a: Expr, b: Expr)
  case Minus(a: Expr, b: Expr)
  case Times(a: Expr, b: Expr)
  case Divide(a: Expr, b: Expr)

// Instantiation of Calculator
object Calculator extends CalculatorInterface:
 import Expr.*
  // Computes the value of each element in a Signal Map
  def computeValues(
      namedExpressions: Map[String, Signal[Expr]]): Map[String, Signal[Double]] =
      namedExpressions.map( (name, expression) =>
        (name, Signal(eval(expression(), namedExpressions)))
      )
  // Defines how to evaluate each expression in an Expression
  def eval(expr: Expr, references: Map[String, Signal[Expr]])(using Signal.Caller): Double =
    expr match {
      case Expr.Literal(dub) => dub
      case Expr.Ref(name) => eval(getReferenceExpr(name, references), references - name)
      case Expr.Plus(a, b) => eval(a, references) + eval(b, references)
      case Expr.Minus(a, b) => eval(a, references) - eval(b, references)
      case Expr.Times(a, b) => eval(a, references) * eval(b, references)
      case Expr.Divide(a, b) => eval(a, references) / eval(b, references)
    }

  // Gets the value from a defined key and defines a new map 
  private def getReferenceExpr(name: String,
      references: Map[String, Signal[Expr]])(using Signal.Caller): Expr =
    references.get(name).fold[Expr] {
      Expr.Literal(Double.NaN)
    } { exprSignal =>
      exprSignal()
    }
