package a14e.bson.encoder

import org.scalatest.{FlatSpec, Matchers}
import BsonEncoder._
import a14e.bson.{Bson, ID}
import a14e.bson.auto._

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
      "name" ->  "name",
      "children" -> Bson.arr()
    )

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
      "name" ->  "name",
      "job" -> Bson.obj(
        "company" -> "some company",
        "salary" -> 123L
      ),
      "children" -> Bson.arr()
    )

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
      "name" ->  "name",
      "children" -> Bson.arr(
        Bson.obj(
          "_id" -> 456,
          "name" ->  "name1",
          "children" -> Bson.arr()
        )
      )
    )

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
        "levelNumber" -> 2
      )
    )

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
    node.asBson shouldBe expectedBson
  }
}
