package a14e.bson.decoder

import java.time.{Instant, LocalDate, ZoneId}
import java.util.Date

import a14e.bson.ID
import a14e.bson.decoder.DecodeStrategy.Named
import a14e.bson.decoder.{BsonDecoder, DecodeStrategy}
import org.bson.types.Decimal128
import org.bson.{BsonArray, BsonBinary, BsonBoolean, BsonDateTime, BsonDecimal128, BsonDocument, BsonDouble, BsonElement, BsonInt32, BsonInt64, BsonNull, BsonString}
import org.scalatest.{FlatSpec, Matchers}

import scala.util.Success


object DecodingSomeEnum extends Enumeration {
  type SomeEnum = Value
  final val SomeEnum1 = Value("SomeEnum123")
  final val SomeEnum2 = Value("SomeEnum234")
}

class DefaultBsonDecodersSpec extends FlatSpec with Matchers {


  "bsonValueDecoder" should "decode same value" in {
    val intValue = 123
    val bsonInt = new BsonInt32(intValue)
    BsonDecoder.bsonValueDecoder.decode(bsonInt) shouldBe Success(bsonInt)
  }


  "stringBsonDecoder" should "decode valid value" in {
    val stringValue = "123"
    val bson = new BsonString(stringValue)
    BsonDecoder.stringBsonDecoder.decode(bson) shouldBe Success(stringValue)

  }

  it should "decode none on invalid value" in {
    val intValue = 123
    val bsonInt = new BsonInt32(intValue)
    BsonDecoder.stringBsonDecoder.decode(bsonInt).isFailure shouldBe true

  }

  "symbolBsonDecoder" should "decode valid value" in {
    val stringValue = "123"
    val bson = new BsonString(stringValue)
    BsonDecoder.symbolBsonDecoder.decode(bson) shouldBe Success(Symbol(stringValue))
  }

  it should "decode none on invalid value" in {
    val intValue = 123
    val bsonInt = new BsonInt32(intValue)
    BsonDecoder.symbolBsonDecoder.decode(bsonInt).isFailure shouldBe true
  }


  "intBsonDecoder" should "decode valid value" in {
    val intValue = 123
    val bsonInt = new BsonInt32(intValue)
    BsonDecoder.intBsonDecoder.decode(bsonInt) shouldBe Success(intValue)
  }

  it should "decode none on invalid value" in {
    val stringValue = "abc"
    val bson = new BsonString(stringValue)
    BsonDecoder.intBsonDecoder.decode(bson).isFailure shouldBe true
  }


  "longBsonDecoder" should "decode valid value" in {
    val x = 123l
    val bson = new BsonInt64(x)
    BsonDecoder.longBsonDecoder.decode(bson) shouldBe Success(x)
  }

  it should "decode none on invalid value" in {
    val x = "abc"
    val bson = new BsonString(x)
    BsonDecoder.longBsonDecoder.decode(bson).isFailure shouldBe true
  }

  "decimal128BsonDecoder" should "decode valid value" in {
    val x = new Decimal128(123l)
    val bson = new BsonDecimal128(x)
    BsonDecoder.decimal128BsonDecoder.decode(bson) shouldBe Success(x)
  }

  it should "decode none on invalid value" in {
    val x = "abc"
    val bson = new BsonString(x)
    BsonDecoder.decimal128BsonDecoder.decode(bson).isFailure shouldBe true
  }


  "doubleBsonDecoder" should "decode valid value" in {
    val x = 123.0
    val bson = new BsonDouble(x)
    BsonDecoder.doubleBsonDecoder.decode(bson) shouldBe Success(x)
  }

  it should "decode none on invalid value" in {
    val x = "abc"
    val bson = new BsonString(x)
    BsonDecoder.doubleBsonDecoder.decode(bson).isFailure shouldBe true
  }


  "booleanBsonDecoder" should "decode valid value" in {
    val x = true
    val bson = new BsonBoolean(x)
    BsonDecoder.booleanBsonDecoder.decode(bson) shouldBe Success(x)
  }

  it should "decode none on invalid value" in {
    val x = "abc"
    val bson = new BsonString(x)
    BsonDecoder.booleanBsonDecoder.decode(bson).isFailure shouldBe true
  }

  "dateBsonDecoder" should "decode valid value" in {
    val x = new Date()
    val bson = new BsonDateTime(x.getTime)
    BsonDecoder.dateBsonDecoder.decode(bson) shouldBe Success(x)
  }

  it should "decode none on invalid value" in {
    val x = "abc"
    val bson = new BsonString(x)
    BsonDecoder.dateBsonDecoder.decode(bson).isFailure shouldBe true
  }

  "instantBsonDecoder" should "decode valid value" in {
    val x = Instant.now
    val bson = new BsonDateTime(x.toEpochMilli)
    BsonDecoder.instantBsonDecoder.decode(bson) shouldBe Success(x)
  }

  it should "decode none on invalid value" in {
    val x = "abc"
    val bson = new BsonString(x)
    BsonDecoder.instantBsonDecoder.decode(bson).isFailure shouldBe true
  }


  "localDateBsonDecoder" should "decode valid value" in {
    val time = LocalDate.now()
    val millis = time.atStartOfDay(ZoneId.of("UTC")).toInstant.toEpochMilli
    val bson = new BsonDateTime(millis)
    BsonDecoder.localDateBsonDecoder.decode(bson) shouldBe Success(time)

  }

  it should "decode none on invalid value" in {
    val x = "abc"
    val bson = new BsonString(x)
    BsonDecoder.localDateBsonDecoder.decode(bson).isFailure shouldBe true
  }


  "bytesBsonDecoder" should "decode valid value" in {
    val bytes = Array[Byte](1, 2, 3)
    val bson = new BsonBinary(bytes)
    BsonDecoder.bytesBsonDecoder.decode(bson).map(_.toSeq) shouldBe Success(bytes.toSeq)
  }

  it should "decode none on invalid value" in {
    val x = "abc"
    val bson = new BsonString(x)
    BsonDecoder.bytesBsonDecoder.decode(bson).isFailure shouldBe true
  }


  "enumBsonDecoder" should "decode valid value" in {
    val bson1 = new BsonString("SomeEnum123")
    BsonDecoder.enumBsonDecoder[DecodingSomeEnum.type].decode(bson1) shouldBe Success(DecodingSomeEnum.SomeEnum1)

    val bson2 = new BsonString("SomeEnum234")
    BsonDecoder.enumBsonDecoder[DecodingSomeEnum.type].decode(bson2) shouldBe Success(DecodingSomeEnum.SomeEnum2)
  }

  it should "decode none on invalid value" in {
    val bson = new BsonString("SomeEnum567")
    BsonDecoder.enumBsonDecoder[DecodingSomeEnum.type].decode(bson).isFailure shouldBe true
  }

  "optionDecoder" should "decode valid value" in {
    val x = "abc"
    val bson = new BsonString(x)
    BsonDecoder.optionDecoder[String].decode(bson) shouldBe Success(Some(x))

  }

  it should "decode Null as none" in {
    BsonDecoder.optionDecoder[String].decode(new BsonNull()) shouldBe Success(None)
  }

  "idDecoder" should "decode valid value" in {
    val x = "abc"
    val bson = new BsonString(x)
    BsonDecoder.idDecoder[String].decode(bson) shouldBe Success(ID(x))

  }

  it should "decode none on invalid value" in {
    BsonDecoder.optionDecoder[String].decode(new BsonNull()) shouldBe Success(None)

  }

  it should "be named with name '_id'" in {
    BsonDecoder.idDecoder[String].decodeStrategies should contain(DecodeStrategy.Named("_id"))
  }

  "seqDecoder" should "decode valid seq" in {
    val value1 = new BsonString("value1")
    val value2 = new BsonString("value2")

    val bson = new BsonArray(java.util.Arrays.asList(value1, value2))

    BsonDecoder.seqDecoder[String].decode(bson) shouldBe Success(Seq("value1", "value2"))

  }

  it should "return none if invalid decoding found" in {
    val value1 = new BsonString("value1")
    val value2 = new BsonString("value2")
    val value3 = new BsonBoolean(false)

    val bson = new BsonArray(java.util.Arrays.asList(value1, value2, value3))

    BsonDecoder.seqDecoder[String].decode(bson).isFailure shouldBe true

  }

  "setDecoder" should "decode valid set" in {
    val value1 = new BsonString("value1")
    val value2 = new BsonString("value2")

    val bson = new BsonArray(java.util.Arrays.asList(value1, value2))

    BsonDecoder.setDecoder[String].decode(bson) shouldBe Success(Set("value1", "value2"))

  }

  it should "return none if invalid decoding found" in {
    val value1 = new BsonString("value1")
    val value2 = new BsonString("value2")
    val value3 = new BsonBoolean(false)

    val bson = new BsonArray(java.util.Arrays.asList(value1, value2, value3))

    BsonDecoder.setDecoder[String].decode(bson).isFailure shouldBe true

  }

  "mapDecoder" should "valid decode map" in {
    val value1 = new BsonString("value1")
    val value2 = new BsonString("value2")

    val bson: BsonDocument =
      new BsonDocument(
        java.util.Arrays.asList(
          new BsonElement("key1", value1),
          new BsonElement("key2", value2)
        )
      )
    BsonDecoder.mapDecoder[String].decode(bson) shouldBe Success(Map("key1" -> "value1", "key2" -> "value2"))
  }

  it should "return none if invalid decoding found" in {
    val value1 = new BsonString("value1")
    val value2 = new BsonString("value2")
    val value3 = new BsonBoolean(false)

    val bson: BsonDocument =
      new BsonDocument(
        java.util.Arrays.asList(
          new BsonElement("key1", value1),
          new BsonElement("key2", value2),
          new BsonElement("key3", value3)
        )
      )

    BsonDecoder.mapDecoder[String].decode(bson).isFailure shouldBe true
  }

  it should "decode named value if needed" in new {
    val value1 = new BsonString("value1")

    val bson: BsonDocument =
      new BsonDocument(
        java.util.Arrays.asList(
          new BsonElement("_id", value1)
        )
      )
    BsonDecoder.mapDecoder[ID[String]].decode(bson) shouldBe Success(Map("_id" -> ID("value1")))
  }
}

