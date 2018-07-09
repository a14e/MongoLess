package a14e.bson.decoder

import a14e.bson.BsonReadExceptionUtils
import org.bson.{BsonDocument, BsonNull, BsonValue}

import scala.util.{Failure, Success}


trait GenericBsonDecoders {

  import shapeless.record._
  import shapeless.LabelledGeneric
  import shapeless._
  import shapeless.labelled._
  import shapeless.Witness


  implicit val bsonReader: BsonDecoder[HNil] = BsonDecoder(_ => Success(HNil))


  implicit def hlistBsonDecoder[Key <: Symbol, Head, Tail <: HList](implicit
                                                                    classFieldKey: Witness.Aux[Key],
                                                                    headDecoder: Lazy[BsonDecoder[Head]],
                                                                    tailDecoder: Lazy[BsonDecoder[Tail]]): BsonDecoder[FieldType[Key, Head] :: Tail] = {

    val enableEmpty = headDecoder.value.enableEmpty
    val key: String = headDecoder.value.replaceName.getOrElse(classFieldKey.value.name)


    BsonDecoder[BsonDocument].flatMapTry { bsonDoc =>
      for {
        tail <- tailDecoder.value.decode(bsonDoc)
        found = bsonDoc.get(key)
        corrected = if (found == null) {
          if (enableEmpty) new BsonNull()
          else throw BsonReadExceptionUtils.missingFieldError(key)
        } else found
        head <- BsonReadExceptionUtils.enrichTryWitKey(headDecoder.value.decode(corrected), key)
      } yield labelled.field[Key][Head](head) :: tail

    }
  }


  implicit def caseClassBsonDecoder[T, Repr](implicit
                                             lgen: LabelledGeneric.Aux[T, Repr],
                                             reprWrites: Lazy[BsonDecoder[Repr]]): BsonDecoder[T] =
    BsonDecoder((obj: BsonValue) => reprWrites.value.decode(obj).map(x => lgen.from(x)))


}
