package a14e.bson.encoder

import org.bson.{BsonNull, BsonValue}


sealed trait WriteAction {
  import WriteAction._

  def flatMap(f: BsonValue => WriteAction): WriteAction = this match {
    case Empty => Empty
    case Value(v) => f(v)
    case NamedValue(_, v) => f(v)
  }

  def extract: BsonValue = this match {
    case Empty => new BsonNull()
    case Value(v) => v
    case NamedValue(_, v) => v
  }

}

object WriteAction {

  def empty: WriteAction = Empty

  case object Empty extends WriteAction
  case class Value(value: BsonValue) extends WriteAction
  case class NamedValue(name: String, value: BsonValue) extends WriteAction

  def enrichWithKey(action: WriteAction, newKey: String): WriteAction = action match {
    case Empty => Empty
    case Value(x) => NamedValue(newKey, x)
    case NamedValue(_, value) => NamedValue(newKey, value)
  }


}