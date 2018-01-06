package a14e.bson.decoder

sealed trait DecodeStrategy

object DecodeStrategy {
  case class Named(key: String) extends DecodeStrategy
  case object EnableEmpty extends DecodeStrategy

  val empty = Seq.empty[DecodeStrategy]
}