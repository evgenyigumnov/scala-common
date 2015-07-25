package igumnov.common

import javax.servlet.http.{HttpServletResponse, HttpServletRequest}

import com.igumnov.common.WebServer
import org.scalatest.FlatSpec
import scala.io.Source

class WebServerTest extends FlatSpec {


  "WebServer" should "work" in {

    WebServer.setPoolSize(1, 20)
    WebServer.init("localhost", 8888)
    WebServer.https("localhost", 8889, "src/test/resources/key.jks", "storepwd", "keypwd")

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
    WebServer.start

    URL.turnOffCertificateValidation
    assert(URL.getStringByUrl("http://localhost:8888") == "Hello")
    assert(URL.getStringByUrl("https://localhost:8889") == "Hello")
    assert(Source.fromURL("http://localhost:8888/rest").mkString == "{\"name\":\"test\",\"value\":1}")

    assert(URL.getStringByUrl("http://localhost:8888/rest", "POST", Option(null), "{\"name\":\"test\",\"value\":1}")
      == "{\"name\":\"test\",\"value\":1}")

    //WebServer.join
  }
}