package a14e.bson.decoder

sealed trait DecodeStrategy

object DecodeStrategy {
  case object Simple extends DecodeStrategy
  case class Named(key: String) extends DecodeStrategy
}