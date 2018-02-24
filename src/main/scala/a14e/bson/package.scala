package a14e

import a14e.bson.decoder.{BsonDecoder, GenericBsonDecoders}
import a14e.bson.encoder.{BsonEncoder, GenericBsonEncoders}
import com.mongodb.async.client.MongoClients
import org.bson.{BsonArray, BsonDocument, BsonValue, Document}

import scala.collection.JavaConverters._
import scala.language.implicitConversions
import scala.util
import scala.util.{Failure, Success, Try}

package object bson {

  object auto extends GenericBsonDecoders with GenericBsonEncoders

  implicit class RichBsonEncodingsObject[T](val obj: T) extends AnyVal {
    def asBson(implicit encoder: BsonEncoder[T]): BsonValue = encoder.encode(obj).extract
  }


  implicit def documentToRichBsonTry(document: Document): RichBsonTryValue = {
    val bsonValueTry = Try {
      document.toBsonDocument(classOf[BsonDocument], MongoClients.getDefaultCodecRegistry)
    }
    new RichBsonTryValue(bsonValueTry)
  }


  implicit class RichBsonValue(val bsonValue: BsonValue) extends AnyVal {

    def decode[T](implicit decoder: BsonDecoder[T]): Try[T] = decoder.decode(bsonValue)

    def asOpt[T](implicit decoder: BsonDecoder[T]): Option[T] = decode[T].toOption

    def as[T](implicit decoder: BsonDecoder[T]): T = decode[T].get

    def search(key: String): Try[BsonValue] = Success(bsonValue).search(key)

    def recursiveSearch(key: String): Try[BsonValue] = Success(bsonValue).recursiveSearch(key)

    def \(key: String): Try[BsonValue] = search(key)

    def \\(key: String): Try[BsonValue] = recursiveSearch(key)
  }

  implicit class RichBsonTryValue(private val value: Try[BsonValue]) extends AnyVal {

    def decode[T](implicit decoder: BsonDecoder[T]): Try[T] = value.flatMap(decoder.decode)

    def asOpt[T](implicit decoder: BsonDecoder[T]): Option[T] = decode[T].toOption

    def as[T](implicit decoder: BsonDecoder[T]): T = decode[T].get


    def search(key: String): Try[BsonValue] = value.flatMap {
      case doc: BsonDocument =>
        val found = doc.get(key)
        if (found == null) Failure(BsonReadExceptionUtils.missingFieldError(key))
        else Success(found)
      case x => Failure(BsonReadExceptionUtils.invalidTypeError[BsonDocument](x))
    }

    def recursiveSearch(key: String): Try[BsonValue] = {
      def findInIterator(iter: Iterator[BsonValue]): Try[BsonValue] = {
        val found =
          iter.collect {
            case doc: BsonDocument => doc
            case array: BsonArray => array
          }
            .map(_ \\ key)
            .find(_.isSuccess)

        found match {
          case Some(success) => success
          case _ => Failure(BsonReadExceptionUtils.missingFieldError(key))
        }
      }


      value.flatMap {
        case doc: BsonDocument =>
          val found = doc.get(key)
          if (found != null) Success(found)
          else findInIterator(doc.values().iterator().asScala)
        case array: BsonArray => findInIterator(array.getValues.iterator().asScala)
        case x => Failure(BsonReadExceptionUtils.invalidTypeError[BsonDocument](x))
      }
    }


    def \(key: String): Try[BsonValue] = search(key)

    def \\(key: String): Try[BsonValue] = recursiveSearch(key)

  }

}
