package a14e.bson.encoder

import org.bson.BsonDocument


trait GenericEncoders {

  import shapeless.labelled._
  import shapeless.{LabelledGeneric, Witness, _}


  implicit lazy val hnilBsonEncoder: BsonEncoder[HNil] = _ => WriteAction.Value(new BsonDocument())


  implicit def hlistBsonEncoder[Key <: Symbol, Head, Tail <: HList](implicit
                                                                    classFieldKey: Witness.Aux[Key],
                                                                    headWrites: Lazy[BsonEncoder[Head]],
                                                                    tailWrites: Lazy[BsonEncoder[Tail]]): BsonEncoder[FieldType[Key, Head] :: Tail] = {
    val classFieldKeyName = classFieldKey.value.name

    hlist =>
      val encoded = tailWrites.value.encode(hlist.tail)
      encoded match {
        case previous@WriteAction.Value(map: BsonDocument) =>
          headWrites.value.encode(hlist.head) match {
            case WriteAction.Value(x) =>
              map.put(classFieldKeyName, x)
              WriteAction.Value(map)

            case WriteAction.NamedValue(key, x) =>
              map.put(key, x)
              WriteAction.Value(map)

            case _ => previous
          }


        case _ =>
          throw new RuntimeException("unsupported hlist read action") // we'll never be here
      }

  }

  implicit def caseClassBsonEncoder[T, Repr](implicit
                                             lgen: LabelledGeneric.Aux[T, Repr],
                                             reprWrites: Lazy[BsonEncoder[Repr]]): BsonEncoder[T] =
    (obj: T) => reprWrites.value.encode(lgen.to(obj))


}