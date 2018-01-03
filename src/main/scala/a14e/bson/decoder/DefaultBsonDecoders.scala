package a14e.bson.decoder

import java.time.{Instant, LocalDate, ZoneId}
import java.util.Date

import a14e.bson.ID
import a14e.bson.util.EnumFinder
import org.bson._
import org.bson.types.Decimal128
import shapeless.Lazy

import scala.collection.JavaConverters._
import scala.collection.immutable.VectorBuilder
import scala.reflect.ClassTag
import scala.util.Try


trait DefaultBsonDecoders {
  implicit lazy val bsonValueDecoder: BsonDecoder[BsonValue] = {
    BsonDecoder(DecodeStrategy.Simple, Some(_))
  }

  implicit lazy val stringBsonDecoder: BsonDecoder[String] = {
    implicitly[BsonDecoder[BsonValue]].collect { case s: BsonString => s.getValue }
  }

  implicit lazy val symbolBsonDecoder: BsonDecoder[Symbol] = {
    implicitly[BsonDecoder[String]].map(Symbol(_))
  }

  implicit lazy val intBsonDecoder: BsonDecoder[Int] = {
    implicitly[BsonDecoder[BsonValue]].collect { case s: BsonNumber => s.intValue() }
  }
  implicit lazy val longBsonDecoder: BsonDecoder[Long] = {
    implicitly[BsonDecoder[BsonValue]].collect { case s: BsonNumber => s.longValue() }
  }
  implicit lazy val decimal128BsonDecoder: BsonDecoder[Decimal128] = {
    implicitly[BsonDecoder[BsonValue]].collect { case s: BsonNumber => s.decimal128Value() }
  }

  implicit lazy val doubleBsonDecoder: BsonDecoder[Double] = {
    implicitly[BsonDecoder[BsonValue]].collect { case s: BsonNumber => s.doubleValue() }
  }

  implicit lazy val booleanBsonDecoder: BsonDecoder[Boolean] = {
    implicitly[BsonDecoder[BsonValue]].collect { case s: BsonBoolean => s.getValue }
  }

  implicit lazy val dateBsonDecoder: BsonDecoder[Date] = {
    implicitly[BsonDecoder[BsonValue]].collect { case s: BsonDateTime => new Date(s.getValue) }
  }

  implicit lazy val instantBsonDecoder: BsonDecoder[Instant] = {
    implicitly[BsonDecoder[BsonValue]].collect { case s: BsonDateTime => Instant.ofEpochMilli(s.getValue) }
  }

  implicit lazy val localDateBsonDecoder: BsonDecoder[LocalDate] = {
    implicitly[BsonDecoder[Instant]].map(i => i.atZone(ZoneId.of("UTC")).toLocalDate)
  }

  implicit lazy val bytesBsonDecoder: BsonDecoder[Array[Byte]] = {
    implicitly[BsonDecoder[BsonValue]].collect { case b: BsonBinary => b.getData }
  }


  implicit def enumBsonDecoder[T <: Enumeration : ClassTag]: BsonDecoder[T#Value] = {
    val enum = EnumFinder.cachedEnum[T]
    implicitly[BsonDecoder[String]].flatMap(x => Try(enum.withName(x)).toOption)
  }


  implicit def optionDecoder[T](implicit decoder: Lazy[BsonDecoder[T]]): BsonDecoder[Option[T]] = {
    implicitly[BsonDecoder[BsonValue]]
      .map {
        case _: BsonNull => None
        case c => decoder.value.decode(c)
      }
  }

  implicit def idDecoder[T](implicit bsonDecoder: BsonDecoder[T]): BsonDecoder[ID[T]] = {
    bsonDecoder.map(ID(_)).copy(decodeStrategy = DecodeStrategy.Named("_id"))
  }

  implicit def seqDecoder[T](implicit bsonDecoder: Lazy[BsonDecoder[T]]): BsonDecoder[Seq[T]] = {
    implicitly[BsonDecoder[BsonValue]].flatMap {
      case s: BsonArray =>
        s.getValues
          .iterator()
          .asScala
          .foldLeft(Option(new VectorBuilder[T])) {
            case (None, _) => None
            case (Some(builder), x) =>
              bsonDecoder.value.decode(x).map(builder += _)
          }.map(_.result())

      case _ => None
    }
  }

  implicit def setDecoder[T](implicit bsonDecoder: Lazy[BsonDecoder[T]]): BsonDecoder[Set[T]] = {
    implicitly[BsonDecoder[BsonValue]].flatMap {
      case s: BsonArray =>
        s.getValues
          .iterator()
          .asScala
          .foldLeft(Option(Set.newBuilder[T])) {
            case (None, _) => None
            case (Some(builder), x) =>
              bsonDecoder.value.decode(x).map(builder += _)
          }.map(_.result())

      case _ => None
    }
  }

  implicit def mapDecoder[T](implicit bsonDecoder: Lazy[BsonDecoder[T]]): BsonDecoder[Map[String, T]] = {
    implicitly[BsonDecoder[BsonValue]].flatMap {
      case s: BsonDocument =>
        bsonDecoder.value match {
          case BsonDecoder(DecodeStrategy.Simple, decodeFunction) =>
            s.entrySet()
              .iterator()
              .asScala
              .foldLeft(Option(Map.newBuilder[String, T])) {
                case (None, _) => None
                case (Some(builder), entry) =>
                  decodeFunction(entry.getValue).map { decoded =>
                    builder += (entry.getKey -> decoded)
                  }
              }.map(_.result())

          case BsonDecoder(DecodeStrategy.Named(key), decodeFunction) =>
            val found = Option(s.get(key)).getOrElse(new BsonNull())
            decodeFunction(found).map(v => Map(key -> v))

          case _ => None
        }
    }
  }

}

