package a14e.bson.decoder


import org.bson.BsonValue

import scala.annotation.implicitNotFound

@implicitNotFound("No bson implicit transformer found for type ${T}. Implement or import an implicit BsonEncoder for this type")
case class BsonDecoder[T](decodeStrategy: DecodeStrategy,
                          decode: BsonValue => Option[T]) {
  self =>

  def map[B](f: T => B): BsonDecoder[B] = copy(decode = self.decode(_).map(f))
  def flatMap[B](f: T => Option[B]): BsonDecoder[B] = copy(decode = self.decode(_).flatMap(f))
  def filter(f: T => Boolean): BsonDecoder[T] = copy(decode = self.decode(_).filter(f))
  def collect[B](f: PartialFunction[T, B]): BsonDecoder[B] = copy(decode = self.decode(_).collect(f))
}

object BsonDecoder extends BsonDecoders


trait BsonDecoders
  extends DefaultBsonDecoders
    with RichObjectBsonDecodingsImplicits
    with GenericDecoders

trait RichObjectBsonDecodingsImplicits {

  implicit class RichBsonDecodingsObject(value: BsonValue) {
    def as[T](implicit decoder: BsonDecoder[T]): Option[T] = decoder.decode(value)
  }

}
