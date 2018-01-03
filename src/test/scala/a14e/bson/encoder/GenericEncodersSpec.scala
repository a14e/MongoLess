package a14e.bson.encoder

import org.scalatest.{FlatSpec, Matchers}
import BsonEncoder._
import a14e.bson.{Bson, ID}


case class SampleUser(id: ID[Int],
                      name: String,
                      job: Option[Job],
                      children: Seq[SampleUser])

case class Job(company: String,
               salary: Long)

class GenericEncodersSpec extends FlatSpec with Matchers {

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

    user.asBsonValue shouldBe expectedBson
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

    user.asBsonValue shouldBe expectedBson
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

    user.asBsonValue shouldBe expectedBson
  }
}
