package a14e.bson.util

import java.util.concurrent.atomic.AtomicReference

import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

object EnumFinder {
  def cachedEnum[T <: Enumeration : ClassTag]: T = {
    val clazz = implicitly[ClassTag[T]]
    enumStore.get().get(clazz) match {
      case None =>
        val c = implicitly[ClassTag[T]].runtimeClass
        val enum = Try(c.getField("MODULE$")) match {
          case Success(m) => m.get(null).asInstanceOf[T]
          case Failure(e) => throw new RuntimeException("Cannot get instance of enum: " + c.getCanonicalName + "; " +
            "make sure the enum is an object and it's not contained in a class or trait", e)
        }
        enumStore.getAndUpdate(_ + (clazz -> enum))
        enum
      case Some(enum) => enum.asInstanceOf[T]
    }
  }
  private val enumStore = new AtomicReference[Map[ClassTag[_], Enumeration]](Map.empty)
}
