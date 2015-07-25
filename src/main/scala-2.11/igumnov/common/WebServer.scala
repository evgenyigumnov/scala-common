package igumnov.common

import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}

import org.eclipse.jetty.server.handler.{ContextHandler, ContextHandlerCollection}
import org.eclipse.jetty.server.{Connector, Handler, ServerConnector, Server}
import org.eclipse.jetty.servlet.{ServletHolder, ServletContextHandler, ServletHandler}
import org.eclipse.jetty.util.thread.QueuedThreadPool

import scala.collection.mutable.ArrayBuffer

object WebServer {

  var threadPool: Option[QueuedThreadPool] = Option(null)
  var server: Option[Server] = Option(null)
  var connector: Option[ServerConnector] = Option(null)
  var handlers: ArrayBuffer[Handler] = ArrayBuffer[Handler]()
  var servletContext: Option[ServletContextHandler] = Option(null)
  var restErrorHandler: Option[(HttpServletRequest, HttpServletResponse, Exception) => JSONClass] = Option(null)

  def setPoolSize(min: Int, max: Int) {
    threadPool = Option(new QueuedThreadPool(max, min))
  }

  def init(hostName: String, port: Int) {

    if (server.isEmpty) {
      if (threadPool.isDefined) {
        server = Option(new Server(threadPool.get))
      } else {
        server = Option(new Server())
      }
    }

    connector = Option(new ServerConnector(server.get))
    connector.get.setHost(hostName)
    connector.get.setPort(port)


  }

  def start {
    server.get.setConnectors(Array[Connector](connector.get))
    val contexts: ContextHandlerCollection = new ContextHandlerCollection
    contexts.setHandlers(handlers.toArray)
    server.get.setHandler(contexts)
    server.get.start

  }

  def join {
    server.get.join

  }

  private def addServlet(s: HttpServlet, name: String) {
    if (servletContext.isEmpty) {
      servletContext = Option(new ServletContextHandler())
      handlers += servletContext.get
    }

    servletContext.get.addServlet(new ServletHolder(s), name)
  }

  def addStringController(path: String, f: (HttpServletRequest, HttpServletResponse) => String) = {
    object StringServlet extends HttpServlet {
      override def doGet(request: HttpServletRequest, response: HttpServletResponse) = {
        response.setHeader("Cache-Control", "no-store")
        response.setHeader("Pragma", "no-cache")
        response.setDateHeader("Expires", 0)
        response.setContentType("text/html")
        response.setStatus(HttpServletResponse.SC_OK)
        response.getWriter().print(f.apply(request, response))
      }

      override def doPost(request: HttpServletRequest, response: HttpServletResponse) = {
        doGet(request, response)
      }

      override def doPut(request: HttpServletRequest, response: HttpServletResponse) = {
        doGet(request, response)
      }

      override def doDelete(request: HttpServletRequest, response: HttpServletResponse) = {
        doGet(request, response)
      }


    }
    addServlet(StringServlet, path)
  }


  def addRestErrorHandler(f: (HttpServletRequest, HttpServletResponse, Exception) => JSONClass) = {
    restErrorHandler = Option(f)
  }

  def addRestController[T: Manifest](path: String, f: (HttpServletRequest, HttpServletResponse, Option[T]) => T) = {
    object RestServlet extends HttpServlet {
      var ret: T = _
      override def doGet(request: HttpServletRequest, response: HttpServletResponse) = {
        try {
          if (List("POST", "PUT") contains request.getMethod) {
            request.getReader
            val jsonString = Stream.continually(request.getReader.readLine()).takeWhile(_ != null).mkString("\n")
            val obj: T = JSON.fromJson[T](jsonString)
            val t: Option[T] = Option(obj)
            ret = f.apply(request, response, t)

          } else {
            ret = f.apply(request, response, null)

          }
          response.setHeader("Cache-Control", "no-store")
          response.setHeader("Pragma", "no-cache")
          response.setDateHeader("Expires", 0)
          response.setContentType("application/json; charset=utf-8")
          response.setStatus(HttpServletResponse.SC_OK)
          response.getWriter().print(JSON.toJson(ret))

        } catch {
          case e: Exception => {
            if(restErrorHandler.isDefined) {
              response.setStatus(HttpServletResponse.SC_BAD_REQUEST)
              response.getWriter().print(JSON.toJson(restErrorHandler.get.apply(request,response,e)))
            }
          }
        }
      }

      override def doPost(request: HttpServletRequest, response: HttpServletResponse) = {
        doGet(request, response)
      }

      override def doPut(request: HttpServletRequest, response: HttpServletResponse) = {
        doGet(request, response)
      }

      override def doDelete(request: HttpServletRequest, response: HttpServletResponse) = {
        doGet(request, response)
      }

    }
    addServlet(RestServlet, path)

  }


  def addServletController(path: String, f: (HttpServletRequest, HttpServletResponse) => Unit) = {
    object StringServlet extends HttpServlet {
      override def doGet(request: HttpServletRequest, response: HttpServletResponse) = {
        f.apply(request, response)
      }

      override def doPost(request: HttpServletRequest, response: HttpServletResponse) = {
        doGet(request, response)
      }

      override def doPut(request: HttpServletRequest, response: HttpServletResponse) = {
        doGet(request, response)
      }

      override def doDelete(request: HttpServletRequest, response: HttpServletResponse) = {
        doGet(request, response)
      }


    }
    addServlet(StringServlet, path)
  }

}


