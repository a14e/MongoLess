package a14e.bson.encoder

import Gender.{Gender, Value}
import a14e.bson.decoder.BsonDecoder
import org.scalatest.{FlatSpec, Matchers}
import a14e.bson.{Bson, ID, auto}
import org.bson.BsonNull


case class SampleUser(id: ID[Int],
                      name: String,
                      job: Option[Job],
                      children: Seq[SampleUser])

case class Job(company: String,
               salary: Long)

case class Level(levelNumber: Int,
                 nextLevel: Option[Level])


case class NamedNode(nodeName: String,
                     children: Map[String, NamedNode])


case class Director(id: ID[String],
                    name: String,
                    gender: Gender,
                    subordinates: Seq[Subordinate])

trait Subordinate

case class Manager(name: String,
                   gender: Gender,
                   salary: BigDecimal) extends Subordinate

case class Clerk(name: String,
                 gender: Gender,
                 salary: BigDecimal) extends Subordinate


object Gender extends Enumeration {
  type Gender = Value
  val Male = Value("Male")
  val Female = Value("Female")
}


class GenericBsonEncodersSpec extends FlatSpec with Matchers {

  "GenericEncoder" should "encode simple class" in {
    val user = SampleUser(
      id = 213,
      name = "name",
      job = None,
      children = Seq.empty
    )

    val expectedBson = Bson.obj(
      "_id" -> 213,
      "name" -> "name",
      "job" -> new BsonNull(),
      "children" -> Bson.arr()
    )

    import a14e.bson.auto._

    user.asBson shouldBe expectedBson
  }

  it should "encode nested classes" in {
    val user = SampleUser(
      id = 213,
      name = "name",
      job = Some(
        Job(
          company = "some company",
          salary = 123
        )
      ),
      children = Seq.empty
    )

    val expectedBson = Bson.obj(
      "_id" -> 213,
      "name" -> "name",
      "job" -> Bson.obj(
        "company" -> "some company",
        "salary" -> 123L
      ),
      "children" -> Bson.arr()
    )

    import a14e.bson.auto._
    user.asBson shouldBe expectedBson
  }

  it should "encode recursive classes" in {
    val user = SampleUser(
      id = 213,
      name = "name",
      job = None,
      children = Seq(
        SampleUser(
          id = 456,
          name = "name1",
          job = None,
          children = Seq.empty
        )
      )
    )

    val expectedBson = Bson.obj(
      "_id" -> 213,
      "name" -> "name",

      "job" -> new BsonNull(),
      "children" -> Bson.arr(
        Bson.obj(
          "_id" -> 456,
          "name" -> "name1",
          "job" -> new BsonNull(),
          "children" -> Bson.arr()
        )
      )
    )

    import a14e.bson.auto._
    user.asBson shouldBe expectedBson
  }

  it should "encode recursive option classes" in {
    val level = Level(
      levelNumber = 1,
      nextLevel = Some(
        Level(
          levelNumber = 2,
          nextLevel = None
        )
      )
    )
    val expectedBson = Bson.obj(
      "levelNumber" -> 1,
      "nextLevel" -> Bson.obj(
        "levelNumber" -> 2,
        "nextLevel" -> new BsonNull()
      )
    )

    import a14e.bson.auto._
    level.asBson shouldBe expectedBson
  }

  it should "encode recursive map classes" in {
    val node =
      NamedNode(
        nodeName = "node1",
        children = Map(
          "node2" -> NamedNode("node2", Map.empty),
          "node3" -> NamedNode("node3", Map.empty)
        )
      )
    val expectedBson = Bson.obj(
      "nodeName" -> "node1",
      "children" -> Bson.obj(
        "node2" -> Bson.obj(
          "nodeName" -> "node2",
          "children" -> Bson.obj()
        ),
        "node3" -> Bson.obj(
          "nodeName" -> "node3",
          "children" -> Bson.obj()
        )
      )
    )

    import a14e.bson.auto._
    node.asBson shouldBe expectedBson
  }


  implicit lazy val subordinateEncoder: BsonEncoder[Subordinate] = {
    import a14e.bson.auto._
    BsonEncoder.switch[String, Subordinate]("type")(
      "Manager" -> BsonEncoder.derived[Manager](),
      "Clerk" -> BsonEncoder.derived[Clerk]()
    )
  }

  it should "work for nested classes with switch" in {
    val director = Director(
      id = "123",
      name = "Director Name",
      gender = Gender.Male,
      subordinates = Seq(
        Manager(
          name = "Manager name",
          gender = Gender.Female,
          salary = 123
        ),
        Clerk(
          name = "Clerk name",
          gender = Gender.Male,
          salary = 456
        )
      )
    )

    val expectedBson = Bson.obj(
      "_id" -> "123",
      "name" -> "Director Name",
      "gender" -> Gender.Male,
      "subordinates" -> Bson.arr(
        Bson.obj(
          "name" -> "Manager name",
          "gender" -> Gender.Female,
          "salary" -> BigDecimal(123),
          "type" -> "Manager"
        ),
        Bson.obj(
          "name" -> "Clerk name",
          "gender" -> Gender.Male,
          "salary" -> BigDecimal(456),
          "type" -> "Clerk"
        )
      )
    )
    import a14e.bson.auto._

    director.asBson shouldBe expectedBson

  }

}
