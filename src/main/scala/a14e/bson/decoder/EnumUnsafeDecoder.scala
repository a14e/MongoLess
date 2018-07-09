package a14e.bson.decoder

import a14e.bson.utils.EnumFinder

import scala.reflect.ClassTag
import scala.util.Try

trait EnumUnsafeDecoder {

  implicit def unsafeEnumBsonDecoder[T <: Enumeration : ClassTag]: BsonDecoder[T#Value] = {
    val enumTry = Try(EnumFinder.cachedEnum[T])
    implicitly[BsonDecoder[String]].flatMapTry(x => enumTry.map(_.withName(x)))
  }

}

object EnumUnsafeDecoder extends EnumUnsafeDecoder