package com.igumnov.scala

import com.igumnov.common.{ORM =>JavaORM}
import com.igumnov.scala.orm.Id
import scala.reflect._

import scala.reflect.ClassTag



object ORM {

  //JavaORM.setIdClass(classOf[Id])

  def findOne[T >: Null : ClassTag]( id: Any): Option[T] =  {
    val className = classTag[T].runtimeClass
    val ret = JavaORM.findOne(className, id)
    if(ret != null) {
      Option(ret.asInstanceOf[T])
    } else {
      Option(null)
    }
  }


  def transaction(t: => Any): Any = {
    t
  }


  def insert[T](obj: Any): T ={
    JavaORM.insert(obj).asInstanceOf[T]
  }

  def applyDDL(path: String) =  {
    JavaORM.applyDDL(path)
  }

  def connectionPool(driverClass: String, url: String, user: String, password: String, minPool: Int, maxPool: Int ) = {

    JavaORM.connectionPool(driverClass,url,user,password,minPool, maxPool)

  }

}
