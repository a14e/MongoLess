package a14e.bson

import org.scalatest.{FlatSpec, Matchers}
import a14e.bson._
import org.bson.Document

import scala.util.Try


class BsonSearchSpec extends FlatSpec with Matchers {


  "search" should "work for first level keys" in {
    val bson = Bson.obj("key" -> "value")
    (bson \ "key").as[String] shouldBe "value"
  }

  it should "not work for first level keys" in {
    val bson = Bson.obj("key" -> "value")
    (bson \ "key2").asOpt[String] shouldBe None
  }

  it should "not work for invalid type" in {
    val bson = Bson.arr("value")
    (bson \ "value").asOpt[String] shouldBe None
  }

  it should "work for first level keys in try" in {
    val bson = Try(Bson.obj("key" -> "value"))
    (bson \ "key").as[String] shouldBe "value"
  }

  it should "not work for first level keys in try" in {
    val bson = Try(Bson.obj("key" -> "value"))
    (bson \ "key2").asOpt[String] shouldBe None
  }

  it should "not work for invalid type in try" in {
    val bson = Try(Bson.arr("value"))
    (bson \ "value").asOpt[String] shouldBe None
  }

  it should "work for first level keys in document" in {
    val bson = new Document("key", "value")
    (bson \ "key").as[String] shouldBe "value"
  }

  it should "not work for first level keys in document" in {
    val bson = new Document("key", "value")
    (bson \ "key2").asOpt[String] shouldBe None
  }

  "recursiveSearch" should "search objects in first level" in {
    val bson = Bson.obj("key" -> "value")
    (bson \\ "key").as[String] shouldBe "value"
  }

  it should "search objects in first level with try" in {
    val bson = Try(Bson.obj("key" -> "value"))
    (bson \\ "key").as[String] shouldBe "value"
  }

  it should "search objects in first level with in document" in {
    val bson = new Document("key", "value")
    (bson \\ "key").as[String] shouldBe "value"
  }


  it should "not find if not exists objects in first level" in {
    val bson = Bson.obj("key" -> "value")
    (bson \\ "key1").asOpt[String] shouldBe None
  }

  it should "not find if not exists objects in first level with try" in {
    val bson = Try(Bson.obj("key" -> "value"))
    (bson \\ "key1").asOpt[String] shouldBe None
  }

  it should "not find if not exists objects in first level with in document" in {
    val bson = new Document("key", "value")
    (bson \\ "key1").asOpt[String] shouldBe None
  }

  it should "find in nested objects" in {
    val bson = Bson.obj("key" -> Bson.obj("key1" -> "value"))
    (bson \\ "key1").as[String] shouldBe "value"
  }

  it should "find in nested objects with try" in {
    val bson = Try(Bson.obj("key" -> Bson.obj("key1" -> "value")))
    (bson \\ "key1").as[String] shouldBe "value"
  }

  it should "find in array" in {
    val bson = Try(Bson.arr(Bson.obj("key1" -> "value")))
    (bson \\ "key1").as[String] shouldBe "value"
  }

  it should "find in inner array" in {
    val bson = Try(Bson.obj("key" -> Bson.arr(Bson.obj("key1" -> "value"))))
    (bson \\ "key1").as[String] shouldBe "value"
  }

  it should "find in nested array" in {
    val bson = Try(Bson.arr(Bson.obj("key" -> Bson.arr(Bson.obj("key1" -> "value")))))
    (bson \\ "key1").as[String] shouldBe "value"
  }

  it should "find in nested objects if there more then one key" in {
    val bson = Try(
      Bson.obj(
        "key3" -> Bson.obj("key4" -> "value"),
        "key" -> Bson.obj(
          "key5" -> "value",
          "key1" -> "value",
          "key6" -> "value"
        )
      )
    )
    (bson \\ "key1").as[String] shouldBe "value"
  }
}
