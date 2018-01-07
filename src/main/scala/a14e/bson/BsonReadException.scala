package a14e.bson

import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

case class BsonReadException(path: Seq[String],
                             error: String) extends Throwable {
  override def getMessage: String = {
    BsonReadExceptionUtils.generateErrorText(path, error)
  }

}


object BsonReadExceptionUtils {
  def generateErrorText(path: Seq[String],
                        error: String): String = {
    val pathString = {
      if (path.isEmpty) ""
      else " at path " + path.mkString(".")
    }
    val text = s"Bson parse error$pathString: $error"
    text
  }


  def invalidTypeError[T: ClassTag](x: Any): BsonReadException = {
    val errorText = s"invalid type of $x. expected ${implicitly[ClassTag[T]].runtimeClass.getCanonicalName}"
    BsonReadException(Nil, errorText)
  }

  def missingFieldError(fieldName: String): BsonReadException = {
    val errorText = "missing field"
    BsonReadException(Seq(fieldName), errorText)
  }

  def enrichTryWitKey[T](tryResult: Try[T],
                         key: String): Try[T] = tryResult.recoverWith {
    case BsonReadException(path, error) => Failure(BsonReadException(key +: path, error))
    case e: Throwable => Failure(BsonReadException(Seq(key), e.getMessage))
  }
}
