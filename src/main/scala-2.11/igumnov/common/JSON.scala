package igumnov.common

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper

class JSONClass {
  def fromJson[T: Manifest] (src:String) = {
    Mapper.get.readValue[T](src)
  }

  def toJson (obj:Any): String = {
    Mapper.get.writeValueAsString(obj)
  }
}
object JSON extends JSONClass

trait JSON [T] extends JSONClass {
  def toJson: String = {
    Mapper.get.writeValueAsString(this)
  }


  def fromJson (src:String):T = {
    Mapper.get.readValue(src, this.getClass).asInstanceOf[T]
  }
}

object Mapper {
  val mapper = new ObjectMapper() with ScalaObjectMapper
  mapper.registerModule(DefaultScalaModule)
  def get: ObjectMapper with ScalaObjectMapper = {
    mapper
  }
}