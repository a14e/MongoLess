# MongoLess
[![Build Status](https://travis-ci.org/a14e/MongoLess.svg?branch=master)](https://travis-ci.org/a14e/MongoLess)
[![codecov.io](https://codecov.io/gh/a14e/MongoLess/coverage.svg?branch=master)](https://codecov.io/gh/MongoLess?branch=master)

Shapeless based BSON serialization for Mongo Java and Scala Drivers


MongoLess is a simple lib for encoding scala case classes for [Mongo Java Driver](https://github.com/mongodb/mongo-java-driver).
You can also use it with [Scala mongo driver](https://github.com/mongodb/mongo-scala-driver)

#Installation

MonadLess is currently available for Scala 2.12 and Java 8.
To install MongoLess just add following line to your sbt file
```scala
libraryDependencies += "com.github.a14e" %% "mongoless" % "0.2.9"
```


# Case class to/from bson encoding

Encoding to Bson is quiet simple: just ```import a14e.bson.auto._``` and call ```asBson```.
For decoding ```import a14e.bson.auto._``` and call ```.as[...]```.
If you want to replace field name with ```_id``` use ```ID``` wrapper.

## Simple example
```scala
import a14e.bson._
import a14e.bson.auto._

case class User(id: ID[Int],
                name: String,
                age: Int)
val exampleUser = User(
  id = 1,
  name = "some name",
  age = 25
)

val bson = exampleUser.asBson
// { "age" : 25, "name" : "some name", "_id" : 1 }
bson.as[User] == exampleUser
// true

```

Nested and recursive case classes are also supported

## Bigger example 
```scala
import a14e.bson._
import a14e.bson.auto._

case class SampleUser(id: ID[Int],
                      name: String,
                      job: Option[Job],
                      children: Seq[SampleUser])

case class Job(company: String,
               salary: Long)

val user = SampleUser(
  id = 213,
  name = "name",
  job = Some(
    Job(
      company = "some company",
      salary = 123
    )
  ),
  children = Seq(
    SampleUser(
      id = 456,
      name = "name1",
      job = None,
      children = Seq.empty
    )
  )
)

val bson = user.asBson

// { "children" : [{ "children" : [], "name" : "name1", "_id" : 456 }], "job" : { "salary" : { "$numberLong" : "123" }, "company" : "some company" }, "name" : "name", "_id" : 213 }

bson.as[SampleUser] == user
// true
```

## Bson builder

MongoLess also offers PlayJson like builder for bson:

```scala
import a14e.bson.Bson
Bson.obj(
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
// { "_id" : 213, "name" : "name", "children" : [{ "_id" : 456, "name" : "name1", "children" : [] }] }
```


## Search and Recursive search
MongoLess also supports helpers for search and recursive search. 
Use `\` to search in root and `\\` for recursive search of nearest node with expected key
```scala
import a14e.bson._
val bson = Bson.obj(
  "_id" -> 213,
  "name" ->  "name",
  "children" -> Bson.arr(
    Bson.obj(
      "_id" -> 456,
      "name" ->  "name1",
      "children" -> Bson.arr(),
      "someKey" -> 123
    )
  )
)
bson \\ "_id"
// Success(BsonInt32{value=213})

(bson \\ "_id").as[Int]
// 213

(bson \ "children" \\ "name").as[String]
// name1

(bson \\ "someKey").as[Int]
// 123

```

## Enum Support
MongoLess also offers limited scala enums support. But enum should be an object and it should
not be nested

```scala
import a14e.bson._
import a14e.bson.auto._
import a14e.bson.auto.enumUnsafe._


object SizeType extends Enumeration {
    type SizeType = Value
    val Big = Value("BIG")
    val Small = Value("SMALL")
}

import SizeType._

case class Hat(price: Int,
               sizeType: SizeType)
val hat = Hat(123, Big)

val bsonHat = hat.asBson
//{ "sizeType" : "BIG", "price" : 123 }
bsonHat.as[Hat] == hat
// true
```


## ADT support
One can build ADT encoders throw `BsonEncoder.switch` (warning: in example lazy val can produce stack overflow)
```scala

trait Shape
case class Circle(r: Double) extends Shape
case class Rectangle(a: Double) extends Shape

import a14e.bson.encoder.BsonEncoder
import a14e.bson._

implicit val shapeEncoder: BsonEncoder[Shape] = {
    import a14e.bson.auto._
    BsonEncoder.switch[String, Shape]("type")(
      "circle" -> BsonEncoder[Circle],
      "rectangle" -> BsonEncoder[Rectangle]
    )
  }

 Circle(1).asBson // { "r" : 1.0, "type" : "circle" }
 Rectangle(1).asBson // { "a" : 1.0, "type" : "rectangle" }

```
and decoders in same way:
```scala

trait Shape
case class Circle(r: Double) extends Shape
case class Rectangle(a: Double) extends Shape

import a14e.bson.decoder.BsonDecoder
import a14e.bson._

implicit val decoder: BsonDecoder[Shape] = {
    import a14e.bson.auto._
    BsonDecoder.switch[String, Shape]("type")(
      "circle" -> BsonDecoder[Circle],
      "rectangle" -> BsonDecoder[Rectangle]
    )
  }


val circleBson = Bson.obj(
  "type" -> "circle",
  "r" -> 1.0
)

circleBson.as[Shape] // == Circle(1.0)

```

## Known Issues
* import of `a14e.bson.auto._` can break encoding of BsonNull
```scala

import org.bson.BsonNull
import a14e.bson._
import a14e.bson.auto._
Bson.obj("key" -> new BsonNull()).asBson // == {"key" : {}}
```
workaround: move move or hide `import a14e.bson.auto._`
```scala

import org.bson.BsonNull
import a14e.bson._
Bson.obj("key" -> new BsonNull()).asBson // == {"key" : null}
```