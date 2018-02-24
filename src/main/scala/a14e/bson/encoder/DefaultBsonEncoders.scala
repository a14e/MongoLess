package a14e.bson.encoder

import java.time.{Instant, LocalDate, ZoneId}
import java.util.Date

import a14e.bson.ID
import org.bson._
import org.bson.types.Decimal128
import shapeless.Lazy

import scala.collection.JavaConverters._


trait DefaultBsonEncoders {
  implicit lazy val stringEncoder: BsonEncoder[String] = BsonEncoder((x: String) => new BsonString(x))
  implicit lazy val symbolEncoder: BsonEncoder[Symbol] = BsonEncoder[String].contramap[Symbol](_.name)
  implicit lazy val intEncoder: BsonEncoder[Int] = BsonEncoder((value: Int) => new BsonInt32(value))
  implicit lazy val longEncoder: BsonEncoder[Long] = BsonEncoder((value: Long) => new BsonInt64(value))
  implicit lazy val decimal128Encoder: BsonEncoder[Decimal128] = BsonEncoder((value: Decimal128) => new BsonDecimal128(value))

  implicit lazy val bigDecimalEncoder: BsonEncoder[BigDecimal] =
    BsonEncoder[Decimal128].contramap[BigDecimal](x => new Decimal128(x.bigDecimal))
  implicit lazy val doubleEncoder: BsonEncoder[Double] = BsonEncoder((value: Double) => new BsonDouble(value))
  implicit lazy val booleanEncoder: BsonEncoder[Boolean] = BsonEncoder((value: Boolean) => new BsonBoolean(value))
  implicit lazy val bytesEncoder: BsonEncoder[Array[Byte]] = BsonEncoder((bytes: Array[Byte]) => new BsonBinary(bytes))

  implicit lazy val dateEncoder: BsonEncoder[Date] = BsonEncoder((x: Date) => new BsonDateTime(x.getTime))
  implicit lazy val instantEncoder: BsonEncoder[Instant] =
    BsonEncoder((x: Instant) => new BsonDateTime(x.toEpochMilli))
  implicit lazy val localDateEncoder: BsonEncoder[LocalDate] =
    implicitly[BsonEncoder[Instant]].contramap[LocalDate] { date =>
    date.atStartOfDay(ZoneId.of("UTC")).toInstant
  }

  implicit def bsonValueEncoder[T <: BsonValue]: BsonEncoder[T] = BsonEncoder[T]((x: T) => x)

  implicit def idBsonEncoder[T](implicit encoder: BsonEncoder[T]): BsonEncoder[ID[T]] = {
    (obj: ID[T]) => encoder.encode(obj).flatMap(b => WriteAction.NamedValue("_id", b))
  }

  implicit def optionBsonEncoder[T](implicit encoder: Lazy[BsonEncoder[T]]): BsonEncoder[Option[T]] = {
    (opt: Option[T]) => opt.fold(WriteAction.empty)(encoder.value.encode)
  }

  implicit def seqBsonEncoder[T](implicit encoder: Lazy[BsonEncoder[T]]): BsonEncoder[Seq[T]] = BsonEncoder {
    (seq: Seq[T]) =>
      val data = seq.toStream.map(encoder.value.encode).collect {
        case WriteAction.Value(x) => x
        case WriteAction.NamedValue(_, x) => x
      }.asJava
     new  BsonArray(data)
  }

  implicit def setBsonEncoder[T](implicit encoder: Lazy[BsonEncoder[T]]): BsonEncoder[Set[T]] = BsonEncoder {
    (seq: Set[T]) =>
      val data = seq.toStream.map(encoder.value.encode).collect {
        case WriteAction.Value(x) => x
        case WriteAction.NamedValue(_, x) => x
      }.asJava
      new BsonArray(data)
  }

  implicit def mapBsonEncoder[T](implicit encoder: Lazy[BsonEncoder[T]]): BsonEncoder[Map[String, T]] = BsonEncoder {
    (map: Map[String, T]) =>
      val data = map.mapValues(encoder.value.encode).toStream.collect {
        case (key, WriteAction.Value(x)) => new BsonElement(key, x)
        case (_, WriteAction.NamedValue(keyForReplace, x)) => new BsonElement(keyForReplace, x)
      }.asJava
      new BsonDocument(data)
  }

}
