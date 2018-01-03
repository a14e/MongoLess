package a14e.bson


import org.bson.{BsonReader, BsonWriter}
import org.bson.codecs.{BsonDocumentCodec, Codec, DecoderContext, EncoderContext}
import scala.reflect.ClassTag

import a14e.bson.decoder.BsonDecoder
import a14e.bson.encoder.BsonEncoder
import a14e.bson.decoder.BsonDecoder._
import a14e.bson.encoder.BsonEncoder._

object Codecs {

  def caseClassCodec[T: BsonEncoder : BsonDecoder : ClassTag]: Codec[T] = {
    val clazz = implicitly[ClassTag[T]].runtimeClass.asInstanceOf[Class[T]]

    new Codec[T] {

      override def decode(reader: BsonReader,
                          decoderContext: DecoderContext): T = {
        val decoded = bsonDocumentCodec.decode(reader, decoderContext).as[T]
        decoded.getOrElse(throw new RuntimeException(s"cant decode value for class $clazz"))
      }

      override def encode(writer: BsonWriter,
                          value: T,
                          encoderContext: EncoderContext): Unit = {
        val bson = value.asBsonValue.asDocument()
        bsonDocumentCodec.encode(writer, bson, encoderContext)
      }

      override def getEncoderClass: Class[T] = clazz
    }
  }

  private val bsonDocumentCodec = new BsonDocumentCodec()
}
