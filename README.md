# MongoLess
[![Build Status](https://travis-ci.org/MongoLess/collz.svg?branch=master)](https://travis-ci.org/a14e/MongoLess)
[![codecov.io](https://codecov.io/gh/a14e/MongoLess/coverage.svg?branch=master)](https://codecov.io/gh/MongoLess/collz?branch=master)

Shapeless based serialization for BSON from Mongo Java Driver


MongoLess is a simple lib for encoding scala case classes for [Mongo Java Driver](https://github.com/mongodb/mongo-java-driver).
You can also use it with [Scala mongo driver](https://github.com/mongodb/mongo-scala-driver)

# Case class to/from bson encoding

Encoding to Bson is quiet simple: just ```import a14e.bson.encoder.BsonEncoder._``` and call ```asBsonValue```.
For decoding ```import a14e.bson.decoder.BsonDecoder._``` and call ```.as[...]```.
If you want to replace field name with ```_id``` use ```ID``` wrapper.


## Simple example
```scala
import a14e.bson.ID
import a14e.bson.encoder.BsonEncoder._
import a14e.bson.decoder.BsonDecoder._

case class User(id: ID[Int],
                name: String,
                age: Int)
val exampleUser = User(
  id = 1,
  name = "some name",
  age = 25
)

val bson = exampleUser.asBsonValue
// { "age" : 25, "name" : "some name", "_id" : 1 }
bson.as[User] == Some(exampleUser)
// true

```

Nested and recursive case classes are also supported

## Bigger example 
```scala
import a14e.bson.ID
import a14e.bson.encoder.BsonEncoder._
import a14e.bson.decoder.BsonDecoder._

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

val bson = user.asBsonValue

// { "children" : [{ "children" : [], "name" : "name1", "_id" : 456 }], "job" : { "salary" : { "$numberLong" : "123" }, "company" : "some company" }, "name" : "name", "_id" : 213 }

bson.as[SampleUser] == Some(user) 
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


## Enum Support
MongoLess also offers limited scala enums support. But enum should be an object and it should
not be contained in a class or trait

```scala
import a14e.bson.encoder.BsonEncoder._
import a14e.bson.decoder.BsonDecoder._


object SizeType extends Enumeration {
    type SizeType = Value
    val Big = Value("BIG")
    val Small = Value("SMALL")
}

import SizeType._

case class Hat(price: Int,
               sizeType: SizeType)
val hat = Hat(123, Big)

val bsonHat = hat.asBsonValue
//{ "sizeType" : "BIG", "price" : 123 }
bsonHat.as[Hat] == Some(hat)
// true
```