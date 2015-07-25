package igumnov.common

import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}

import org.eclipse.jetty.http.HttpVersion
import org.eclipse.jetty.server.handler.{ContextHandler, ContextHandlerCollection}
import org.eclipse.jetty.server._
import org.eclipse.jetty.servlet.{ServletHolder, ServletContextHandler, ServletHandler}
import org.eclipse.jetty.util.ssl.SslContextFactory
import org.eclipse.jetty.util.thread.QueuedThreadPool

import scala.collection.mutable.ArrayBuffer

object WebServer {

  var threadPool: Option[QueuedThreadPool] = Option(null)
  var server: Option[Server] = Option(null)
  var connector: Option[ServerConnector] = Option(null)
  var https: Option[ServerConnector] = Option(null)
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

  def https(hostName: String, port: Int, keystoreFile: String, storePassword: String, managerPassword: String) {
    if (server.isEmpty) {
      if (threadPool.isDefined) {
        server = Option(new Server(threadPool.get))
      }
      else {
        server = Option(new Server)
      }
    }
    val http_config: HttpConfiguration = new HttpConfiguration
    http_config.setSecureScheme("https")
    http_config.setSecurePort(port)
    val sslContextFactory: SslContextFactory = new SslContextFactory
    sslContextFactory.setKeyStorePath(keystoreFile)
    sslContextFactory.setKeyStorePassword(storePassword)
    sslContextFactory.setKeyManagerPassword(managerPassword)
    val https_config: HttpConfiguration = new HttpConfiguration(http_config)
    https_config.addCustomizer(new SecureRequestCustomizer)
    https = Option(new ServerConnector(server.get, new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString), new HttpConnectionFactory(https_config)))
    https.get.setPort(port)
    https.get.setHost(hostName)
  }

  def start {
    if(https.isDefined && connector.isDefined) {
      server.get.setConnectors(Array[Connector](connector.get, https.get))
    } else {
      if(connector.isDefined) {
        server.get.setConnectors(Array[Connector](connector.get))
      } else {
        server.get.setConnectors(Array[Connector](https.get))
      }
    }
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


