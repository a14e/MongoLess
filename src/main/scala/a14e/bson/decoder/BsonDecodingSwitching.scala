package a14e.bson.decoder

import org.bson.BsonDocument

trait BsonDecodingSwitching {


  def switch[KEY: BsonDecoder, T](keyPath: String)(decoders: (KEY, BsonDecoder[_ <: T])*): BsonDecoder[T] = {
    import a14e.bson._

    val decodersMap: Map[KEY, BsonDecoder[_ <: T]] = decoders.toMap
    BsonDecoder[BsonDocument].flatMapTry { doc =>
      for {
        key <- (doc \ keyPath).decode[KEY]
        decoder = decodersMap.getOrElse[BsonDecoder[_ <: T]](key,
          throw BsonReadException(keyPath :: Nil, s"Not found decoder by key $key")
        )
        result <- decoder.decode(doc)
      } yield result
    }
  }
}
