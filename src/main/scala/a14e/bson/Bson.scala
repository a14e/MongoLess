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
    elems.foldLeft(new BsonDocument()) {
      case (doc, (key, BsonWrapperImpl(WriteAction.Value(x)))) =>
        doc.put(key, x)
        doc
      case (doc,(_, BsonWrapperImpl(WriteAction.NamedValue(keyForReplace, x)))) =>
        doc.put(keyForReplace, x)
        doc
      case (doc, _) => doc
    }
  }

  def arr(elems: BsonWrapper*): BsonArray = {
    val array = elems.toStream.collect {
      case BsonWrapperImpl(WriteAction.Value(x)) => x
      case BsonWrapperImpl(WriteAction.NamedValue(_, x)) => x
    }.asJava
    new BsonArray(array)
  }


}

