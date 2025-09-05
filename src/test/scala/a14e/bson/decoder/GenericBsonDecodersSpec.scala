package a14e.bson.decoder

import a14e.bson.{Bson, ID}
import a14e.bson._
import a14e.bson.auto._
import org.scalatest.{FlatSpec, Matchers}
import BsonDecoder._
import org.bson.BsonString

import scala.util.Success

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

case class ClassWithList(sequence: List[Int])
case class ClassWithVector(sequence: Vector[Int])

class GenericBsonDecodersSpec extends FlatSpec with Matchers {

  "GenericDecoders" should "encode simple class" in {
    val user = SampleUser(
      id = 213,
      name = "name",
      job = None,
      children = Seq.empty
    )

    val bson = Bson.obj(
      "_id" -> 213,
      "name" ->  "name",
      "children" -> Bson.arr()
    )

    bson.as[SampleUser] shouldBe user
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

    val bson = Bson.obj(
      "_id" -> 213,
      "name" ->  "name",
      "job" -> Bson.obj(
        "company" -> "some company",
        "salary" -> 123L
      ),
      "children" -> Bson.arr()
    )

    bson.as[SampleUser] shouldBe user
  }

  it should "encode recourcive classes" in {
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

    val bson = Bson.obj(
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

    bson.as[SampleUser] shouldBe user
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
    val bson = Bson.obj(
      "levelNumber" -> 1,
      "nextLevel" -> Bson.obj(
        "levelNumber" -> 2
      )
    )

    bson.as[Level] shouldBe level
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
    val bson = Bson.obj(
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
    bson.as[NamedNode] shouldBe node
  }

  it should "encode classes with different sequence types" in {

    val nodeWithList =
      ClassWithList(
        sequence = List(1,2,3)
      )

    val nodeWithVector =
      ClassWithVector(
        sequence = Vector(1,2,3)
      )

    val bson = Bson.obj(
      "sequence" -> Bson.arr(1, 2, 3)
    )

    bson.as[ClassWithList] shouldBe nodeWithList
    bson.as[ClassWithVector] shouldBe nodeWithVector
  }
}
