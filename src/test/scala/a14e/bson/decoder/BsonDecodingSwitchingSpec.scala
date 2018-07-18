package a14e.bson.decoder

import a14e.bson._
import org.scalatest.{FlatSpec, Matchers}
import scala.util.Success

import a14e.bson.auto._

trait Shape
case class Circle(r: Double) extends Shape
case class Rectangle(a: Double) extends Shape


class BsonDecodingSwitchingSpec extends FlatSpec with Matchers {

  implicit val decoder: BsonDecoder[Shape] = {
    BsonDecoder.switch[String, Shape]("type")(
      "circle" -> BsonDecoder[Circle],
      "rectangle" -> BsonDecoder[Rectangle]
    )
  }


  "BsonDecoder.switch" should "decode valid value" in {
    val circle = Circle(1.0)
    val bson = Bson.obj(
      "type" -> "circle",
      "r" -> circle.r
    )
    bson.as[Shape] shouldBe circle
  }

  it should "fail if decoder not found by key" in {
    val bson = Bson.obj(
      "type" -> "triangle",
      "d" -> "1"
    )

    intercept[BsonReadException] {
      bson.as[Shape]
    }
  }

}

