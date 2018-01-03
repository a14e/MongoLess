package a14e.bson.encoder

import java.time.{Instant, LocalDate, ZoneId}
import org.bson.BsonValue
import shapeless.Lazy

import scala.annotation.implicitNotFound


@implicitNotFound("No bson implicit transformer found for type ${T}. Implement or import an implicit BsonEncoder for this type")
trait BsonEncoder[T] {
  def encode(obj: T): WriteAction

  def contramap[B](to: B => T): BsonEncoder[B] = x => encode(to(x))
}

object BsonEncoder extends BsonEncoders {
  def apply[T](f: T => BsonValue): BsonEncoder[T] = obj => WriteAction.Value(f(obj))
}

trait BsonEncoders
  extends DefaultBsonEncoders
    with GenericEncoders
    with RichObjectBsonEncodingsImplicits


trait RichObjectBsonEncodingsImplicits {

  implicit class RichBsonEncodingsObject[T](obj: T) {
    def asBsonValue(implicit encoder: BsonEncoder[T]): BsonValue = encoder.encode(obj).extract
  }

}