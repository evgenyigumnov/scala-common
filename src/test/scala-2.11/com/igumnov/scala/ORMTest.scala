package com.igumnov.scala


import org.scalatest._
import ORM._

class ORMTest extends FlatSpec {


  "ORM" should "work" in {

    FolderOps.createIfNotExists("tmp")
    FolderOps.createIfNotExists("tmp/sql_folder")

    ORM.connectionPool("org.h2.Driver", "jdbc:h2:mem:test", "SA", "", 10, 30)
    FileOps.writeString("CREATE TABLE ObjectDTO (id BIGINT AUTO_INCREMENT PRIMARY KEY)\n", "tmp/sql_folder/1.sql")
    FileOps.appendLine("ALTER TABLE ObjectDTO ADD name VARCHAR(255)", "tmp/sql_folder/1.sql")
    ORM.applyDDL("tmp/sql_folder")

    FileOps.writeString("ALTER TABLE ObjectDTO ADD salary INT", "tmp/sql_folder/2.sql")
    ORM.applyDDL("tmp/sql_folder")

    transaction {
      var obj = new ObjectDTO()
      obj.name = "aaa"
      obj = insert[ObjectDTO](obj)
      val ret = findOne[ObjectDTO](obj.id)
      assert(ret.get.name == "aaa")

      obj.name="b"
      update[ObjectDTO](obj)

      val ret2 = findOne[ObjectDTO](obj.id)
      assert(ret2.get.name == "b")
      ret2.get.name="aaa"
      update[ObjectDTO](ret2.get)

      val retList = findAll[ObjectDTO]
      assert(retList.head.name == "aaa")
      val retList2 = findBy[ObjectDTO]("name=?","aaa")
      assert(retList2.head.name == "aaa")
      delete(obj)
      val emptyList = findAll[ObjectDTO]
      assert(emptyList.size == 0)

    }

  }

}
