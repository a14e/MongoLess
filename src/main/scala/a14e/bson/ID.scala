package a14e.bson

import scala.language.implicitConversions
import scala.reflect.ClassTag

trait ID[T] {
  def value: T
}

object ID {
  implicit def apply[T](x: T): ID[T] = new ID[T] {

    override def value: T = x

    override def hashCode(): Int = value.hashCode()

    override def equals(obj: scala.Any): Boolean = obj match {
      case c: ID[_] => c.value == this.value
      case _ => obj == value
    }

    override def toString: String = s"ID($value)"
  }


  implicit def toValue[T](id: ID[T]): T = id.value
}

