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


  implicit val bsonReader: BsonDecoder[HNil] = BsonDecoder(DecodeStrategy.empty, _ => Success(HNil))


  implicit def hlistBsonDecoder[Key <: Symbol, Head, Tail <: HList](implicit
                                                                    classFieldKey: Witness.Aux[Key],
                                                                    headDecoder: Lazy[BsonDecoder[Head]],
                                                                    tailDecoder: Lazy[BsonDecoder[Tail]]): BsonDecoder[FieldType[Key, Head] :: Tail] = {

    val enableEmpty = headDecoder.value.decodeStrategies.collectFirst { case DecodeStrategy.EnableEmpty =>}.isDefined

    val key: String = headDecoder.value.decodeStrategies.collectFirst {
      case DecodeStrategy.Named(name) => name
    }.getOrElse(classFieldKey.value.name)


    BsonDecoder(DecodeStrategy.empty, {
      case bsonDoc: BsonDocument =>

        for {
          tail <- tailDecoder.value.decode(bsonDoc)
          found = bsonDoc.get(key)
          corrected = if (found == null) {
            if(enableEmpty) new BsonNull()
            else throw BsonReadExceptionUtils.missingFieldError(key)
          } else found
          head <- BsonReadExceptionUtils.enrichTryWitKey(headDecoder.value.decode(corrected), key)
        } yield labelled.field[Key][Head](head) :: tail

      case x => Failure(BsonReadExceptionUtils.invalidTypeError[BsonDocument](x))
    })
  }


  implicit def caseClassBsonDecoder[T, Repr](implicit
                                             lgen: LabelledGeneric.Aux[T, Repr],
                                             reprWrites: Lazy[BsonDecoder[Repr]]): BsonDecoder[T] =
    BsonDecoder(DecodeStrategy.empty, (obj: BsonValue) => reprWrites.value.decode(obj).map(x => lgen.from(x)))


}
