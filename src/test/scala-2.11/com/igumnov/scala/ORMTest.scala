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
      obj.name="aaa"
      obj = ORM.insert[ObjectDTO](obj)
      val ret = ORM.findOne[ObjectDTO](obj.id)
      assert(ret.get.name== "aaa")
    }

  }

}
