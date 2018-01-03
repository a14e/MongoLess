package a14e.bson.decoder

import java.time.{Instant, LocalDate, ZoneId}
import java.util.Date

import a14e.bson.ID
import a14e.bson.encoder.{BsonEncoder, WriteAction}
import org.bson.types.Decimal128
import org.bson.{BsonArray, BsonBinary, BsonBoolean, BsonDateTime, BsonDecimal128, BsonDocument, BsonDouble, BsonElement, BsonInt32, BsonInt64, BsonString}
import org.scalatest.{FlatSpec, Matchers}

object EncodingSomeEnum extends Enumeration {
  type SomeEnum = Value
  final val SomeEnum1 = Value("SomeEnum123")
  final val SomeEnum2 = Value("SomeEnum234")
}

class DefaultBsonEncodersSpec extends FlatSpec with Matchers {

  "stringEncoder" should "encode valid value" in {
    val x = "123"
    val expected = WriteAction.Value(new BsonString(x))
    val result = BsonEncoder.stringEncoder.encode(x)
    result shouldBe expected
  }

  "symbolEncoder" should "encode valid value" in {
    val x = 'abc
    val expected = WriteAction.Value(new BsonString(x.name))
    val result = BsonEncoder.symbolEncoder.encode(x)
    result shouldBe expected
  }

  "intEncoder" should "encode valid value" in {
    val x = 123
    val expected = WriteAction.Value(new BsonInt32(x))
    val result = BsonEncoder.intEncoder.encode(x)
    result shouldBe expected

  }

  "longEncoder" should "encode valid value" in {
    val x = 123L
    val expected = WriteAction.Value(new BsonInt64(x))
    val result = BsonEncoder.longEncoder.encode(x)
    result shouldBe expected
  }

  "decimal128Encoder" should "encode valid value" in {
    val x = new Decimal128(123L)
    val expected = WriteAction.Value(new BsonDecimal128(x))
    val result = BsonEncoder.decimal128Encoder.encode(x)
    result shouldBe expected
  }

  "doubleEncoder" should "encode valid value" in {
    val x = 123.0
    val expected = WriteAction.Value(new BsonDouble(x))
    val result = BsonEncoder.doubleEncoder.encode(x)
    result shouldBe expected

  }

  "booleanEncoder" should "encode valid value" in {
    val x = true
    val expected = WriteAction.Value(new BsonBoolean(x))
    val result = BsonEncoder.booleanEncoder.encode(x)
    result shouldBe expected
  }

  "bytesEncoder" should "encode valid value" in {
    val x = Array[Byte](1, 2, 3)
    val expected = WriteAction.Value(new BsonBinary(x))
    val result = BsonEncoder.bytesEncoder.encode(x)
    result shouldBe expected

  }

  "dateEncoder" should "encode valid value" in {
    val x = new Date()
    val expected = WriteAction.Value(new BsonDateTime(x.getTime))
    val result = BsonEncoder.dateEncoder.encode(x)
    result shouldBe expected
  }

  "instantEncoder" should "encode valid value" in {
    val x = Instant.now()
    val expected = WriteAction.Value(new BsonDateTime(x.toEpochMilli))
    val result = BsonEncoder.instantEncoder.encode(x)
    result shouldBe expected
  }

  "localDateEncoder" should "encode valid value" in {
    val x = LocalDate.now()
    val millis = x.atStartOfDay(ZoneId.of("UTC")).toInstant.toEpochMilli
    val expected = WriteAction.Value(new BsonDateTime(millis))
    val result = BsonEncoder.localDateEncoder.encode(x)
    result shouldBe expected
  }

  "bsonValueEncoder" should "pass same value" in {
    val x = new BsonInt32(123)
    val expected = WriteAction.Value(x)
    val result = BsonEncoder.bsonValueEncoder[BsonInt32].encode(x)
    result shouldBe expected
  }

  "idBsonEncoder" should "make value named" in {
    val x = ID(123)
    val expected = WriteAction.NamedValue("_id", new BsonInt32(x))
    val result = BsonEncoder.idBsonEncoder[Int].encode(x)
    result shouldBe expected

  }

  "enumEncoder" should "encode valid value" in {
    val x = EncodingSomeEnum.SomeEnum1
    val expected = WriteAction.Value(new BsonString("SomeEnum123"))
    val result = BsonEncoder.enumEncoder[EncodingSomeEnum.type].encode(x)
    result shouldBe expected
  }

  "optionBsonEncoder" should "pass valid value if some" in {
    val value = 123
    val option = Some(value)
    val expected = WriteAction.Value(new BsonInt32(value))
    val result = BsonEncoder.optionBsonEncoder[Int].encode(option)
    result shouldBe expected
  }

  it should "pass empty if none" in {
    val option = Option.empty[Int]
    val expected = WriteAction.Empty
    val result = BsonEncoder.optionBsonEncoder[Int].encode(option)
    result shouldBe expected
  }

  "seqBsonEncoder" should "encode valid seq" in {
    val xs = Seq(1, 2, 3)
    val expected = WriteAction.Value(
      new BsonArray(java.util.Arrays.asList(new BsonInt32(1), new BsonInt32(2), new BsonInt32(3)))
    )
    val result = BsonEncoder.seqBsonEncoder[Int].encode(xs)
    result shouldBe expected
  }

  it should "skip empty values" in {
    val xs = Seq(Option(1), Option(2), Option.empty[Int])
    val expected = WriteAction.Value(
      new BsonArray(java.util.Arrays.asList(new BsonInt32(1), new BsonInt32(2)))
    )
    val result = BsonEncoder.seqBsonEncoder[Option[Int]].encode(xs)
    result shouldBe expected
  }

  "setBsonEncoder" should "encode valid seq" in {
    val xs = Set(1, 2, 3)
    val expected = WriteAction.Value(
      new BsonArray(java.util.Arrays.asList(new BsonInt32(1), new BsonInt32(2), new BsonInt32(3)))
    )
    val result = BsonEncoder.setBsonEncoder[Int].encode(xs)
    result shouldBe expected

  }

  it should "skip empty values" in {
    val xs = Set(Option(1), Option(2), Option.empty[Int])
    val expected = WriteAction.Value(
      new BsonArray(java.util.Arrays.asList(new BsonInt32(1), new BsonInt32(2)))
    )
    val result = BsonEncoder.setBsonEncoder[Option[Int]].encode(xs)
    result shouldBe expected

  }


  "mapBsonEncoder" should "encode valid seq" in {

    val value1 = new BsonString("value1")
    val value2 = new BsonString("value2")
    val expected =
        WriteAction.Value(
          new BsonDocument(
            java.util.Arrays.asList(
              new BsonElement("key1", value1),
              new BsonElement("key2", value2)
            )
        )
      )


    val xs = Map("key1" -> "value1", "key2" -> "value2")
    val result = BsonEncoder.mapBsonEncoder[String].encode(xs)
    result shouldBe expected
  }

  it should "skip empty values" in {
    val value1 = new BsonString("value1")
    val expected =
      WriteAction.Value(
        new BsonDocument(
          java.util.Arrays.asList(
            new BsonElement("key1", value1)
          )
        )
      )


    val xs = Map("key1" -> Option("value1"), "key2" -> Option.empty[String])
    val result = BsonEncoder.mapBsonEncoder[Option[String]].encode(xs)
    result shouldBe expected

  }

  it should "should replace name for specific fields" in {
    val value1 = new BsonString("value1")
    val expected =
      WriteAction.Value(
        new BsonDocument(
          java.util.Arrays.asList(
            new BsonElement("_id", value1)
          )
        )
      )


    val xs = Map("key1" -> ID("value1"))
    val result = BsonEncoder.mapBsonEncoder[ID[String]].encode(xs)
    result shouldBe expected
  }
}
