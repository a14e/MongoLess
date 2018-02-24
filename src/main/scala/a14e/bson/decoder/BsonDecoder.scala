package a14e.bson.decoder

import org.bson.BsonValue

import scala.annotation.implicitNotFound
import scala.util.{Success, Try, Failure}

@implicitNotFound("No bson implicit transformer found for type ${T}. Implement or import an implicit BsonEncoder for this type")
case class BsonDecoder[T](decodeStrategies: Seq[DecodeStrategy],
                          decode: BsonValue => Try[T]) {
  self =>

  def map[B](f: T => B): BsonDecoder[B] = copy(decode = self.decode(_).map(f))

  def flatMap[B](f: T => Try[B]): BsonDecoder[B] = copy(decode = self.decode(_).flatMap(f))

  def filter(f: T => Boolean, error: T => Throwable): BsonDecoder[T] =
    transform {
      case Success(x) if f(x) => Success(x)
      case Success(x) => Failure(error(x))
      case err@Failure(_) => err
    }

  def transform[B](f: Try[T] => Try[B]): BsonDecoder[B] = copy(decode = { x => Try(f(decode(x))).flatten })

  def collect[B](f: PartialFunction[T, B], error: T => Throwable): BsonDecoder[B] =
    transform {
      case Success(x) if f.isDefinedAt(x) => Success(f(x))
      case Success(x) => Failure(error(x))
      case err@Failure(_) => err.asInstanceOf[Try[B]]
    }

}

object BsonDecoder extends DefaultBsonDecoders {
  def apply[T: BsonDecoder]: BsonDecoder[T] = implicitly[BsonDecoder[T]]
}


