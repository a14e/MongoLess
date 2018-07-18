package a14e.bson.decoder

import a14e.bson.{BsonReadException, auto}
import org.bson.{BsonDocument, BsonValue}
import shapeless.{LabelledGeneric, Lazy}

import scala.annotation.implicitNotFound
import scala.util.{Failure, Success, Try}

@implicitNotFound("No bson implicit transformer found for type ${T}. Implement or import an implicit BsonEncoder for this type")
case class BsonDecoder[T](decode: BsonValue => Try[T],
                          replaceName: Option[String] = None,
                          enableEmpty: Boolean = false) {
  self =>

  def map[B](f: T => B): BsonDecoder[B] = copy(decode = self.decode(_).map(f))

  def flatMapTry[B](f: T => Try[B]): BsonDecoder[B] = copy(decode = self.decode(_).flatMap(f))

  def flatMap[B](f: T => BsonDecoder[B]): BsonDecoder[B] = {
    def newDecodeFunction(bson: BsonValue): Try[B] = {
      for {
        newDecoder <- self.decode(bson).map(f)
        value <- newDecoder.decode(bson)
      } yield value
    }

    copy(decode = newDecodeFunction)
  }

  def filter(f: T => Boolean, error: T => Throwable = filterFail(_)): BsonDecoder[T] =
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

  private def filterFail(x: Any): Throwable = BsonReadException(Nil, s"Object $x does not pass filter condition")
}

object BsonDecoder extends DefaultBsonDecoders with BsonDecodingSwitching {

  def apply[T: BsonDecoder]: BsonDecoder[T] = implicitly[BsonDecoder[T]]

  def fromTry[T](tryValue: Try[T]): BsonDecoder[T] = BsonDecoder(_ => tryValue)

  def single[T](x: T): BsonDecoder[T] = fromTry(Success(x))

  def failed[T](err: Throwable): BsonDecoder[T] = fromTry(Failure(err))

  def derived[T <: Product with Serializable] = new DummyApplyDecoderWrapper[T]

  class DummyApplyDecoderWrapper[T <: Product with Serializable] {
    def apply[Repr]()(implicit
                      lgen: LabelledGeneric.Aux[T, Repr],
                      reprWrites: Lazy[BsonDecoder[Repr]]): BsonDecoder[T] = auto.caseClassBsonDecoder[T, Repr]
  }
}


