package com.igumnov.scala

import com.igumnov.common.orm.Id

//import com.igumnov.scala.orm.Id


class ObjectDTO {

  @Id(autoIncremental = true)
  var id:Long = _
  var name:String = _
  var salary: Int = _

}
