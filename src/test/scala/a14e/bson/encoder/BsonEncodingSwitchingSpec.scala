package a14e.bson.encoder


import a14e.bson._
import org.scalatest.{FlatSpec, Matchers}
import a14e.bson.auto._

trait Shape
case class Circle(r: Double) extends Shape
case class Rectangle(a: Double) extends Shape
case class Triangle(h: Double, d: Double) extends Shape

class BsonEncodingSwitchingSpec extends FlatSpec with Matchers {

  implicit val encoder = {
    import a14e.bson.auto._
    BsonEncoder.switch[String, Shape]("type")(
      "circle" -> BsonEncoder.derived[Circle](),
      "rectangle" -> BsonEncoder.derived[Rectangle]()
    )
  }

  "BsonEncoder.switch" should "encode encode valid value" in {
    val circle = Circle(1.0)
    val bson = Bson.obj(
      "type" -> "circle",
      "r" -> circle.r
    )

    circle.asBson shouldBe bson
  }

  it should "fail if not encoder found" in {
    val circle = Triangle(1.0, 2.0)
    intercept[RuntimeException] {
      circle.asBson
    }

  }

  it should "fail if not encodes to object" in {
    implicit val decoder: BsonEncoder[Integer] = {
      BsonEncoder.switch[String, Integer]("type")(
        "int" -> BsonEncoder[Int].contramap[Integer](Integer.valueOf(_))
      )
    }

    intercept[UnsupportedOperationException] {
      new RichBsonEncodingsObject(Integer.valueOf(1)).asBson(decoder)
    }
  }
}
