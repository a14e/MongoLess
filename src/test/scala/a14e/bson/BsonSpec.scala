package a14e.bson


import org.bson.{BsonArray, BsonBoolean, BsonDocument, BsonElement, BsonInt32, BsonString, Document}
import org.scalatest.{FlatSpec, Matchers}

class BsonSpec extends FlatSpec with Matchers {

  "bson.obj" should "encode objects" in {

    val value1 = new BsonString("value1")
    val value2 = new BsonInt32(2)
    val value3 = new BsonBoolean(false)

    val expected: BsonDocument =
      new BsonDocument(
        java.util.Arrays.asList(
          new BsonElement("key1", value1),
          new BsonElement("key2", value2),
          new BsonElement("key3", value3)
        )
      )


    val result: BsonDocument = Bson.obj(
      "key1" -> "value1",
      "key2" -> 2,
      "key3" -> false
    )

    expected shouldBe result
  }

  it should "replace name for id" in {
    val value1 = new BsonString("value1")

    val expected: BsonDocument =
      new BsonDocument(
        java.util.Arrays.asList(
          new BsonElement("_id", value1)
        )
      )

    val result = Bson.obj(
      "key1" -> ID("value1")
    )

    expected shouldBe result
  }

  "bson.arr" should "encode arrays" in {
    val value1 = new BsonString("value1")
    val value2 = new BsonInt32(2)
    val value3 = new BsonBoolean(false)

    val expected = new BsonArray(java.util.Arrays.asList(value1, value2, value3))

    val result = Bson.arr("value1", 2, false)

    expected shouldBe result
  }


}
