package a14e.bson.encoder

import java.time.{Instant, LocalDate, ZoneId}

import a14e.bson.auto
import org.bson.{BsonDocument, BsonValue}
import shapeless.{LabelledGeneric, Lazy}

import scala.annotation.implicitNotFound
import scala.language.implicitConversions
import scala.reflect.ClassTag


@implicitNotFound("No bson implicit transformer found for type ${T}. Implement or import an implicit BsonEncoder for this type")
trait BsonEncoder[-T] {
  def encode(obj: T): WriteAction

  def contramap[B](to: B => T): BsonEncoder[B] = x => encode(to(x))
}

object BsonEncoder extends DefaultBsonEncoders with BsonEncodingSwitching {
  def apply[T: BsonEncoder]: BsonEncoder[T] = implicitly[BsonEncoder[T]]

  def apply[T](f: T => BsonValue): BsonEncoder[T] = obj => WriteAction.Value(f(obj))


  def derived[T <: Product with Serializable] = new DummyApplyEncoderWrapper[T]

  class DummyApplyEncoderWrapper[T <: Product with Serializable] {
    def apply[Repr]()(implicit
                      lgen: LabelledGeneric.Aux[T, Repr],
                      reprWrites: Lazy[BsonEncoder[Repr]]): BsonEncoder[T] = auto.caseClassBsonEncoder[T, Repr]
  }
}

