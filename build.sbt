name := "MongoLess"

version := "0.1"

scalaVersion := "2.12.4"

libraryDependencies ++= Seq(
  "com.chuusai" %% "shapeless" % "2.3.2",
  "org.mongodb" % "mongodb-driver-async" % "3.6.1",


  "org.scalatest" %% "scalatest" % "3.0.1" % "test"
)
