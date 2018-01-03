package a14e.bson.decoder

import org.bson.{BsonDocument, BsonNull, BsonValue}


trait GenericDecoders {

  import shapeless.record._
  import shapeless.LabelledGeneric
  import shapeless._
  import shapeless.labelled._
  import shapeless.Witness


  implicit val bsonReader: BsonDecoder[HNil] =  BsonDecoder(DecodeStrategy.Simple, _ => Some(HNil))


  implicit def hlistBsonDecoder[Key <: Symbol, Head, Tail <: HList](implicit
                                                                    classFieldKey: Witness.Aux[Key],
                                                                    headDecoder: Lazy[BsonDecoder[Head]],
                                                                    tailDecoder: Lazy[BsonDecoder[Tail]]): BsonDecoder[FieldType[Key, Head] :: Tail] = {
    // TODO more informative errors

    val key: String = headDecoder.value.decodeStrategy match {
      case DecodeStrategy.Simple => classFieldKey.value.name
      case DecodeStrategy.Named(name) => name
    }

    BsonDecoder(DecodeStrategy.Simple, {
      case bsonDoc: BsonDocument =>

        val found = bsonDoc.get(key)
        val corrected = if (found == null) new BsonNull() else found

        for {
          head <- headDecoder.value.decode(corrected)
          tail <- tailDecoder.value.decode(bsonDoc)
        } yield labelled.field[Key][Head](head) :: tail

      case _ => None
    })
  }



  implicit def caseClassBsonDecoder[T, Repr](implicit
                                             lgen: LabelledGeneric.Aux[T, Repr],
                                             reprWrites: Lazy[BsonDecoder[Repr]]): BsonDecoder[T] =
    BsonDecoder(DecodeStrategy.Simple, (obj: BsonValue) => reprWrites.value.decode(obj).map(x => lgen.from(x)))


}
