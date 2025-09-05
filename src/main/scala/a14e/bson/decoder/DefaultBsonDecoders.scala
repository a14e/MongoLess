package a14e.bson.decoder

import java.time.{Instant, LocalDate, ZoneId}
import java.util.Date

import a14e.bson.{BsonReadException, BsonReadExceptionUtils, ID}
import org.bson.{BsonValue, _}
import org.bson.types.Decimal128
import shapeless.Lazy

import scala.collection.JavaConverters._
import scala.collection.generic.CanBuildFrom
import scala.collection.immutable.VectorBuilder
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}


trait DefaultBsonDecoders {
  implicit lazy val bsonValueDecoder: BsonDecoder[BsonValue] = BsonDecoder(Success(_))

  implicit lazy val bsonDocumentDecoder: BsonDecoder[BsonDocument] = BsonDecoder[BsonDocument] ({
    case bsonDoc: BsonDocument => Success(bsonDoc)
    case x => Failure(BsonReadExceptionUtils.invalidTypeError[BsonDocument](x))
  }:BsonValue => Try[BsonDocument])


  implicit lazy val stringBsonDecoder: BsonDecoder[String] = {
    BsonDecoder[BsonValue].collect({ case s: BsonString => s.getValue }, failError[BsonString])
  }

  implicit lazy val symbolBsonDecoder: BsonDecoder[Symbol] = {
    BsonDecoder[String].map(Symbol(_))
  }


  implicit lazy val BigDecimalDecoder: BsonDecoder[BigDecimal] = {
    BsonDecoder[Decimal128].map(_.bigDecimalValue())
  }

  implicit lazy val intBsonDecoder: BsonDecoder[Int] = {
    BsonDecoder[BsonValue]
      .collect({ case s: BsonNumber => s.intValue() }, failError[BsonNumber])
  }
  implicit lazy val longBsonDecoder: BsonDecoder[Long] = {
    BsonDecoder[BsonValue]
      .collect({ case s: BsonNumber => s.longValue() }, failError[BsonNumber])
  }
  implicit lazy val decimal128BsonDecoder: BsonDecoder[Decimal128] = {
    BsonDecoder[BsonValue]
      .collect({ case s: BsonNumber => s.decimal128Value() }, failError[BsonNumber])
  }

  implicit lazy val doubleBsonDecoder: BsonDecoder[Double] = {
    BsonDecoder[BsonValue]
      .collect({ case s: BsonNumber => s.doubleValue() }, failError[BsonNumber])
  }

  implicit lazy val booleanBsonDecoder: BsonDecoder[Boolean] = {
    BsonDecoder[BsonValue]
      .collect({ case s: BsonBoolean => s.getValue }, failError[BsonBoolean])
  }

  implicit lazy val dateBsonDecoder: BsonDecoder[Date] = {
    BsonDecoder[BsonValue]
      .collect({ case s: BsonDateTime => new Date(s.getValue) }, failError[BsonDateTime])
  }

  implicit lazy val instantBsonDecoder: BsonDecoder[Instant] = {
    BsonDecoder[BsonValue]
      .collect({ case s: BsonDateTime => Instant.ofEpochMilli(s.getValue) }, failError[BsonDateTime])
  }

  implicit lazy val localDateBsonDecoder: BsonDecoder[LocalDate] = {
    implicitly[BsonDecoder[Instant]].map(i => i.atZone(ZoneId.of("UTC")).toLocalDate)
  }

  implicit lazy val bytesBsonDecoder: BsonDecoder[Array[Byte]] = {
    BsonDecoder[BsonValue]
      .collect({ case b: BsonBinary => b.getData }, failError[BsonBinary])
  }

  def enumBsonDecoder[T <: Enumeration](enum: T): BsonDecoder[T#Value] = {
    implicitly[BsonDecoder[String]].map(x => enum.withName(x))
  }


  implicit def optionDecoder[T](implicit decoder: Lazy[BsonDecoder[T]]): BsonDecoder[Option[T]] = {
    BsonDecoder[BsonValue]
      .flatMapTry {
        case _: BsonNull => Success(Option.empty[T])
        case c => decoder.value.decode(c).map(Some(_))
      }.copy(enableEmpty = true)
  }

  implicit def idDecoder[T](implicit bsonDecoder: BsonDecoder[T]): BsonDecoder[ID[T]] = {
    bsonDecoder.map(ID(_)).copy(replaceName = Some("_id"))
  }

  implicit def seqDecoder[T, S[_] <: Seq[_]](implicit bsonDecoder: Lazy[BsonDecoder[T]], cbf: CanBuildFrom[S[T], T, S[T]]): BsonDecoder[S[T]] = {
    BsonDecoder[BsonValue].flatMapTry {
      case s: BsonArray =>
        s.getValues
          .iterator()
          .asScala
          .foldLeft(Try(new VectorBuilder[T])) { (builderTry, current) =>

            for {
              builder <- builderTry
              decoded <- bsonDecoder.value.decode(current)
            } yield builder += decoded
          }.map(_.result().to[S])

      case x => fail[BsonArray](x)
    }
  }

  implicit def setDecoder[T](implicit bsonDecoder: Lazy[BsonDecoder[T]]): BsonDecoder[Set[T]] = {
    BsonDecoder[BsonValue].flatMapTry {
      case s: BsonArray =>
        s.getValues
          .iterator()
          .asScala
          .foldLeft(Try(Set.newBuilder[T])) { (builderTry, current) =>
            for {
              builder <- builderTry
              decoded <- bsonDecoder.value.decode(current)
            } yield builder += decoded
          }.map(_.result())

      case x => fail[BsonArray](x)
    }
  }

  implicit def mapDecoder[T](implicit bsonDecoder: Lazy[BsonDecoder[T]]): BsonDecoder[Map[String, T]] = {
    val fixedKeyOpt: Option[String] = bsonDecoder.value.replaceName
    lazy val enableEmpty = bsonDecoder.value.enableEmpty

    lazy val decodeFunction: BsonValue => Try[T] = bsonDecoder.value.decode

    fixedKeyOpt match {
      case Some(key) =>
        BsonDecoder[BsonValue].flatMapTry {
          case s: BsonDocument =>
            val decoded = Option(s.get(key)) match {
              case Some(v) => decodeFunction(v)
              case None if enableEmpty => decodeFunction(new BsonNull())
              case None =>
                throw BsonReadExceptionUtils.missingFieldError(key)
            }
            val enrichedError = BsonReadExceptionUtils.enrichTryWitKey(decoded, key)
            enrichedError.map(v => Map(key -> v))
          case x => fail[BsonDocument](x)
        }

      case None =>
        BsonDecoder[BsonValue].flatMapTry {
          case s: BsonDocument =>
            s.entrySet()
              .iterator()
              .asScala
              .foldLeft(Try(Map.newBuilder[String, T])) { (builderTry, entry) =>
                for {
                  builder <- builderTry
                  decoded <- BsonReadExceptionUtils
                    .enrichTryWitKey(decodeFunction(entry.getValue), entry.getKey)
                } yield builder += (entry.getKey -> decoded)

              }.map(_.result())
          case x => fail[BsonDocument](x)
        }
    }
  }

  private def failError[T: ClassTag](x: BsonValue): Throwable = BsonReadExceptionUtils.invalidTypeError[T](x)

  private def fail[T: ClassTag](x: BsonValue): Try[Nothing] = Failure(failError[T](x))

}

