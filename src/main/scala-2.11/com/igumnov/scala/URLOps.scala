package com.igumnov.scala

import java.util

import com.igumnov.common.{ URL => cURL}

object URLOps {
    def turnOffCertificateValidation {
      cURL.turnOffCertificateValidation()
    }

    def getStringByUrl(url:String): String = {
      cURL.getAllToString(url)
    }

    def getStringByUrl(url: String,method: String, postParam: Option[Map[String, AnyRef]], postBody: String): String = {
      import java.util.{Map => JavaMap}
      import java.util.HashMap

      var params: JavaMap[String, AnyRef] = new util.HashMap[String, AnyRef]()
      if(postParam isDefined) {
        postParam.get.foreach(p => {
          params.put(p._1, p._2)
        })
      }

      cURL.getAllToString(url,method,params,postBody)
    }
}
