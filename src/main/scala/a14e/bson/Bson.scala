package a14e.bson

import a14e.bson.decoder.GenericBsonDecoders
import a14e.bson.encoder.{BsonEncoder, GenericBsonEncoders, WriteAction}
import org.bson.{BsonArray, BsonDocument, BsonElement}

import scala.collection.JavaConverters._

object Bson {

  sealed trait BsonWrapper
  private case class BsonWrapperImpl(writeAction: WriteAction) extends BsonWrapper

  implicit def objToWrapper[T](obj: T)(implicit encoder: BsonEncoder[T]): BsonWrapper = {
    BsonWrapperImpl(encoder.encode(obj))
  }

  def obj(elems: (String, BsonWrapper)*): BsonDocument = {
    val bsonValues = elems.toStream.collect {
      case (key, BsonWrapperImpl(WriteAction.Value(x))) => new BsonElement(key, x)
      case (_, BsonWrapperImpl(WriteAction.NamedValue(keyForReplace, x))) => new BsonElement(keyForReplace, x)
    }.asJava
    new BsonDocument(bsonValues)
  }

  def arr(elems: BsonWrapper*): BsonArray = {
    val array = elems.toStream.collect {
      case BsonWrapperImpl(WriteAction.Value(x)) => x
      case BsonWrapperImpl(WriteAction.NamedValue(_, x)) => x
    }.asJava
    new BsonArray(array)
  }


}

