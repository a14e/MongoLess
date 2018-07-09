package a14e.bson.encoder

import org.bson.{BsonNull, BsonValue}


sealed trait WriteAction {
  import WriteAction._

  def flatMap(f: BsonValue => WriteAction): WriteAction = this match {
    case Empty => Empty
    case Value(v) => f(v)
    case NamedValue(_, v) => f(v)
  }

  def map(f: BsonValue => BsonValue): WriteAction = this match {
    case Empty => Empty
    case Value(v) => Value(f(v))
    case NamedValue(k, v) => NamedValue(k, f(v))
  }

  def extract: BsonValue = this match {
    case Empty => new BsonNull()
    case Value(v) => v
    case NamedValue(_, v) => v
  }

}

object WriteAction {
  case object Empty extends WriteAction
  case class Value(value: BsonValue) extends WriteAction
  case class NamedValue(name: String, value: BsonValue) extends WriteAction
}