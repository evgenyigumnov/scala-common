package com.igumnov.scala

import java.util

import com.igumnov.common.orm.Transaction
import com.igumnov.common.{ORM =>JavaORM}
import com.igumnov.scala.orm.Id
import scala.collection.mutable.ArrayBuffer
import scala.reflect._

import scala.reflect.ClassTag



object ORM {
  JavaORM.setIdClass(classOf[Id])

  //TODO It is not thread safe :( need to fix
  var threadTransactions = Map[String, Transaction]()

  def findOne[T >: Null : ClassTag]( id: Any): Option[T] =  {

    val className = classTag[T].runtimeClass
    val threadName = Thread.currentThread().getName
    val transaction = threadTransactions.get(threadName)
    var ret:AnyRef = null
    if(transaction.isDefined) {
      ret = transaction.get.findOne(className, id)
    } else {
      ret = JavaORM.findOne(className, id)
    }
    if(ret != null) {
      Option(ret.asInstanceOf[T])
    } else {
      Option(null)
    }
  }

  def findAll[T: ClassTag](): List[T] =  {

    val className = classTag[T].runtimeClass
    val threadName = Thread.currentThread().getName
    val transaction = threadTransactions.get(threadName)
    var ret:util.ArrayList[AnyRef] = null
    if(transaction.isDefined) {
      ret = transaction.get.findAll(className)
    } else {
      ret = JavaORM.findAll(className)
    }

    import collection.JavaConversions._
    ret.map(o => {
      o.asInstanceOf[T]
    }).toList


  }

  def findBy[T: ClassTag](where:String, params: AnyRef *): List[T] =  {

    val className = classTag[T].runtimeClass
    val threadName = Thread.currentThread().getName
    val transaction = threadTransactions.get(threadName)
    var ret:util.ArrayList[AnyRef] = null
    var javaParams = ArrayBuffer[AnyRef]()

    params.foreach(p => {
      javaParams += p
    })

    if(transaction.isDefined) {
      ret = transaction.get.findBy(where, className, javaParams.toArray:_*)
    } else {
      ret = JavaORM.findBy(where, className, javaParams.toArray:_*)
    }

    import collection.JavaConversions._
    ret.map(o => {
      o.asInstanceOf[T]
    }).toList


  }


  def transaction(t: => Any): Any = {
    val threadName = Thread.currentThread().getName
    try {
      val transaction = JavaORM.beginTransaction()
      threadTransactions += threadName -> transaction
      t
      transaction.commit()
    } finally {
      threadTransactions-threadName
    }
  }


  def update[T](obj: Any): T ={
    val threadName = Thread.currentThread().getName
    val transaction = threadTransactions.get(threadName)
    if(transaction.isDefined) {
      transaction.get.update(obj).asInstanceOf[T]
    } else {
      JavaORM.update(obj).asInstanceOf[T]
    }
  }


  def insert[T](obj: Any): T ={
    val threadName = Thread.currentThread().getName
    val transaction = threadTransactions.get(threadName)
    if(transaction.isDefined) {
      transaction.get.insert(obj).asInstanceOf[T]
    } else {
      JavaORM.insert(obj).asInstanceOf[T]
    }
  }

  def delete[T](obj: Any): Int ={
    val threadName = Thread.currentThread().getName
    val transaction = threadTransactions.get(threadName)
    if(transaction.isDefined) {
      transaction.get.delete(obj)
    } else {
      JavaORM.delete(obj)
    }
  }

  def applyDDL(path: String) =  {
    JavaORM.applyDDL(path)
  }

  def connectionPool(driverClass: String, url: String, user: String, password: String, minPool: Int, maxPool: Int ) = {

    JavaORM.connectionPool(driverClass,url,user,password,minPool, maxPool)

  }

}
