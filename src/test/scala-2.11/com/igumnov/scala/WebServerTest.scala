package com.igumnov.scala

import javax.servlet.http.{HttpServletResponse, HttpServletRequest}

import com.igumnov.scala.webserver.User
import org.scalatest.FlatSpec
import scala.io.Source

class WebServerTest extends FlatSpec {


  "WebServer" should "work" in {

    WebServer.setPoolSize(1, 20)
    WebServer.init("localhost", 8888)
    WebServer.https("localhost", 8889, "src/test/resources/key.jks", "storepwd", "keypwd")


    WebServer.loginService((name) => {
      if (name == "admin") {
        Option(new User("admin", "CRYPT:adpexzg3FUZAk", Option(Array[String]("user"))))
      } else {
        Option(null)
      }
    })

    WebServer.securityPages("/login", "/login", "/logout")

    WebServer.addStringController("/login", (rq, rs) => {
      "<form method='POST' action='/j_security_check'>" + "<input type='text' name='j_username'/>" +
        "<input type='password' name='j_password'/>" + "<input type='submit' value='Login'/></form>"
    })


    WebServer.addAllowRule("/*")
    WebServer.addRestrictRule("/new", Array[String]("user"))
    WebServer.addStringController("/new", (rq, rs) => {
      "new new"
    })

    WebServer.addStringController("/", (rq, rs) => {
      "Hello"
    })
    WebServer.addRestController[SampleEntity]("/rest", (rq, rs, obj) => {
      rq.getMethod match {
        case "GET" => {
          val ret = new SampleEntity()
          ret.name = "test"
          ret.value = 1
          ret
        }
        case "POST" => {
          obj.get
        }
      }
    })

    WebServer.addRestController[SampleEntity]("/err", (rq, rs, obj) => {
      throw new Exception("err")
    })

    WebServer.addRestErrorHandler((rq: HttpServletRequest, rs: HttpServletResponse, e: Exception) => {
      val ret = new SampleEntity()
      ret.name = e.getMessage
      ret
    })


    FolderOps.createIfNotExists("tmp")
    FileOps.writeString("hello.world=Hello world\n", "tmp/locale_en.properties")
    FileOps.appendLine("params=1{0}", "tmp/locale_en.properties")
    FileOps.writeString("hello.world=Привет мир", "tmp/locale_ru.properties")

    val langs = Map[String, String]("ru" -> "tmp/locale_ru.properties",
      "en" -> "tmp/locale_en.properties")

    WebServer.locale(langs, (rq,rs)=>{
      "en"
    })

    WebServer.templates("tmp",0)

    FileOps.writeString("<html><body><span th:text=\"${varName}\"></span><span th:text=\"#{hello.world}\"></span></body><html>", "tmp/example.html")
    WebServer.addController("/index", (rq, rs, model) => {
      model += "varName" -> "123"
      "example"
    })
    WebServer.addController("/lang", (rq, rs, model) => {
      model += "varName" -> WebServer.getMessage(rq,rs,"hello.world",Option(null))
      "example"
    })

    WebServer.addController("/param", (rq, rs, model) => {
      model += "varName" -> WebServer.getMessage(rq,rs,"params",Option(List[String]("2")))
      "example"
    })
    WebServer.start
    //WebServer.join

    URLOps.turnOffCertificateValidation
    assert(URLOps.getStringByUrl("http://localhost:8888/param") == "<html><head></head><body><span>12</span><span>Hello world</span></body></html>")
    assert(URLOps.getStringByUrl("http://localhost:8888/lang") == "<html><head></head><body><span>Hello world</span><span>Hello world</span></body></html>")
    assert(URLOps.getStringByUrl("http://localhost:8888/index") == "<html><head></head><body><span>123</span><span>Hello world</span></body></html>")
    assert(URLOps.getStringByUrl("http://localhost:8888") == "Hello")
    assert(URLOps.getStringByUrl("https://localhost:8889") == "Hello")
    assert(Source.fromURL("http://localhost:8888/rest").mkString == "{\"name\":\"test\",\"value\":1}")

    assert(URLOps.getStringByUrl("http://localhost:8888/rest", "POST", Option(null), "{\"name\":\"test\",\"value\":1}")
      == "{\"name\":\"test\",\"value\":1}")
    //WebServer.join

  }
}