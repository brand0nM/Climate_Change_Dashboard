package codecs


// Instantiate Type for constructing JSON objects
enum Json:
  /** The JSON `null` value */
  case Null
  /** JSON boolean values */
  case Bool(value: Boolean)
  /** JSON numeric values */
  case Num(value: BigDecimal)
  /** JSON string values */
  case Str(value: String)
  /** JSON objects */
  case Obj(fields: Map[String, Json])
  /** JSON arrays */
  case Arr(items: List[Json])
  /** Method for decoding JSON constructs */
  def decodeAs[A](using decoder: Decoder[A]): Option[A] = decoder.decode(this)


// Turns a value of type `A` into its JSON representation
trait Encoder[-A]:
  def encode(value: A): Json
    /**
    * Transforms this `Encoder[A]` into an `Encoder[B]`, given a transformation function
    * from `B` to `A`.
    *
    * For instance, given a `Encoder[String]`, we can get an `Encoder[UUID]`:
    *
    * {{{
    *   def uuidEncoder(given stringEncoder: Encoder[String]): Encoder[UUID] =
    *     stringEncoder.transform[UUID](uuid => uuid.toString)
    * }}}
    *
    * This operation is also known as “contramap”.
    */
  def transform[B](f: B => A): Encoder[B] =
    Encoder.fromFunction[B](value => this.encode(f(value)))

// Cases for Encoding different JSON types
trait EncoderInstances:
  /** An encoder for the `Unit` value */
  given Encoder[Unit] =
    Encoder.fromFunction(_ => Json.Null)
  /** An encoder for `Int` values */
  given Encoder[Int] =
    Encoder.fromFunction(n => Json.Num(BigDecimal(n)))
  /** An encoder for `String` values */
  given Encoder[String] =
    Encoder.fromFunction(str => Json.Str(str))
  /** An encoder for `Boolean` values */
  given Encoder[Boolean] =
    Encoder.fromFunction(bool => Json.Bool(bool))
  /**
    * Encodes a list of values of type `A` into a JSON array containing
    * the list elements encoded with the given `encoder`
    */
  given [A] (using encoder: Encoder[A]): Encoder[List[A]] = 
    Encoder.fromFunction(as => Json.Arr(as.map(encoder.encode)))

// Instantiated JSON Encoder
object Encoder extends EncoderInstances:
  // Method for creating an instance of encoder from a function `f`
  def fromFunction[A](f: A => Json) = new Encoder[A]:
    def encode(value: A): Json = f(value)


// An `Encoder` that returns only JSON objects
trait ObjectEncoder[-A] extends Encoder[A]:
  // Refines the encoding result to `Json.Obj`
  def encode(value: A): Json.Obj
  // Acts as a Map Reduce, combining value pairs a key will map to
  def zip[B](that: ObjectEncoder[B]): ObjectEncoder[(A, B)] =
    ObjectEncoder.fromFunction { case (a, b) =>
      Json.Obj(this.encode(a).fields ++ that.encode(b).fields)
    }

// Instantiated Object Encoder
object ObjectEncoder:
  // Method for creating an object encoder from a function `f`   
  def fromFunction[A](f: A => Json.Obj): ObjectEncoder[A] = new ObjectEncoder[A]:
    def encode(value: A): Json.Obj = f(value)
  // An encoder for values of type `A` producing a JSON object with one field
  // Encoding the value and maping from its named type
  def field[A](name: String)(using encoder: Encoder[A]): ObjectEncoder[A] =
    ObjectEncoder.fromFunction(a => Json.Obj(Map(name -> encoder.encode(a))))


// Decodes a serialized value into its initial type `A`
trait Decoder[+A]:
  // decodes JSON, returning `Some`, or `None`
  def decode(data: Json): Option[A]
  // combines Decoders returning a pair of decoded value if both succeed, or None
  def zip[B](that: Decoder[B]): Decoder[(A, B)] =
    Decoder.fromFunction { json =>
      this.decode(json).zip(that.decode(json))
    }
  // Essentially a map operation on existing JSON type from A => B
  def transform[B](f: A => B): Decoder[B] =
    Decoder.fromFunction(json => this.decode(json).map(f))

// Cases for Decoding different JSON types
trait DecoderInstances:
  /** A decoder for the `Unit` value */
  given Decoder[Unit] =
    Decoder.fromPartialFunction { case Json.Null => () }
  /** A decoder for `Int` values. Hint: use the `isValidInt` method of `BigDecimal` */
  given Decoder[Int] =
    Decoder.fromPartialFunction { case Json.Num(n) if n.isValidInt => n.intValue}
  /** A decoder for `String` values */
  given Decoder[String] =
    Decoder.fromPartialFunction { case Json.Str(s) => s}
  /** A decoder for `Boolean` values */
  given Decoder[Boolean] =
    Decoder.fromPartialFunction { case Json.Bool(bool) => bool}
  /** A decoder for JSON arrays,succeeds if all items are successfully decoded */
  given [A] (using decoder: Decoder[A]): Decoder[List[A]] = 
    Decoder.fromFunction {
      case Json.Arr(list) => 
          list.foldLeft(Option(List.empty[A])) {(acc, og) =>
            acc match {
              case Some(ls) => decoder.decode(og).map(ls :+ _)
              case _ => Option(List.empty[A])
            }
        }
      case _ => None
    }
  // Decodes the value of a field using supplied `name` and a given `decoder`.
  def field[A](name: String)(using decoder: Decoder[A]): Decoder[A] =
     Decoder.fromFunction{
      case Json.Obj(field) if (field.contains(name)) => field(name).decodeAs[A]
      case _ => None
    }

// Instantiated JSON Decoder
object Decoder extends DecoderInstances:
  // Method to build a decoder instance from a function `f`
  def fromFunction[A](f: Json => Option[A]): Decoder[A] = new Decoder[A]:
    def decode(data: Json): Option[A] = f(data)
  // Alternative method for creating decoder instances
  def fromPartialFunction[A](pf: PartialFunction[Json, A]): Decoder[A] =
    fromFunction(pf.lift)


case class Person(name: String, age: Int)
object Person extends PersonCodecs
trait PersonCodecs:
  /** The encoder for `Person` */
  given Encoder[Person] =
    ObjectEncoder.field[String]("name")
      .zip(ObjectEncoder.field[Int]("age"))
      .transform[Person](user => (user.name, user.age))
  /** The corresponding decoder for `Person` */
  given Decoder[Person] =
    Decoder.field[String]("name")
      .zip(Decoder.field[Int]("age"))
      .transform[Person]((name, age) => Person(name, age))


case class Contacts(people: List[Person])
object Contacts extends ContactsCodecs
trait ContactsCodecs: 
  // The JSON representation of a value of type `Contacts` should be
  // a JSON object with a single field named “people” containing an
  // array of values of type `Person` (reuse the `Person` codecs)
  given Encoder[Contacts] =
    ObjectEncoder.field[List[Person]]("people")
      .transform[Contacts](contacts => contacts.people)
  /** The corresponding decoder for `Person` */
  given Decoder[Contacts] =
    Decoder.field[List[Person]]("people")
      .transform[Contacts](person => Contacts(person))


// In case you want to try the code, this is a simple `Main`.
object Main:
  import Util.*
  def main(args: Array[String]): Unit =
    println(renderJson(42))
    println(renderJson("foo"))
    val maybeJsonString = parseJson(""" "foo" """)
    val maybeJsonObj    = parseJson(""" { "name": "Alice", "age": 42 } """)
    val maybeJsonObj2   = parseJson(""" { "name": "Alice", "age": "42" } """)
    // Uncomment the following lines as you progress in the assignment
    println(maybeJsonString.flatMap(_.decodeAs[Int]))
    println(maybeJsonString.flatMap(_.decodeAs[String]))
    println(maybeJsonObj.flatMap(_.decodeAs[Person]))
    println(maybeJsonObj2.flatMap(_.decodeAs[Person]))
    println(renderJson(Person("Bob", 66)))