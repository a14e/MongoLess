package a14e.bson.encoder

import org.bson.BsonDocument

import scala.language.implicitConversions
import scala.reflect.ClassTag

trait BsonEncodingSwitching {


  def switch[KEY: BsonEncoder, T <: AnyRef](keyPath: String)
                                           (pairs: BsonEncoderWrapper[KEY, _ <: T]*): BsonEncoder[T] = {
    import a14e.bson._

    val encoders = pairs.map(x => x.clazz -> x.asInstanceOf[BsonEncoderWrapper[KEY, AnyRef]]).toMap

    obj: T =>
      val clazz = obj.getClass.asInstanceOf[Class[AnyRef]]
      val encoderWrapper = encoders.getOrElse(clazz, throw new RuntimeException(s"Not found encoder for class $clazz"))

      encoderWrapper.encoder.encode(obj.asInstanceOf[AnyRef]).map {
        case bson: BsonDocument =>
          bson.append(keyPath, encoderWrapper.key.asBson)
          bson
        case _ =>
          throw new UnsupportedOperationException("Encoder switching supported only for encoding to documents")
      }
  }


  implicit def objectPairToWrapper[KEY, T <: AnyRef : ClassTag](pair: (KEY, BsonEncoder[T])): BsonEncoderWrapper[KEY, T] = {
    val (key, encoder) = pair
    val clazz = implicitly[ClassTag[T]].runtimeClass.asInstanceOf[Class[AnyRef]]
    BsonEncoderWrapper(key, clazz, encoder.asInstanceOf[BsonEncoder[AnyRef]])
  }

  protected case class BsonEncoderWrapper[KEY, T <: AnyRef](key: KEY,
                                                            clazz: Class[AnyRef],
                                                            encoder: BsonEncoder[AnyRef])

}

