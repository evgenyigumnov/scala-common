package igumnov.common

import java.io.IOException
import java.security.Principal
import javax.security.auth.Subject
import javax.servlet.{ServletException, ServletRequest}
import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}

import com.igumnov.common.webserver.{ControllerContext, MessageResolver}
import igumnov.common.webserver.User
import nz.net.ultraq.thymeleaf.LayoutDialect
import org.eclipse.jetty.http.HttpVersion
import org.eclipse.jetty.security.MappedLoginService.{UserPrincipal, RolePrincipal, KnownUser}
import org.eclipse.jetty.security.authentication.FormAuthenticator
import org.eclipse.jetty.security._
import org.eclipse.jetty.server.handler.{ContextHandler, ContextHandlerCollection}
import org.eclipse.jetty.server._
import org.eclipse.jetty.server.session.SessionHandler
import org.eclipse.jetty.servlet.{DefaultServlet, ServletHolder, ServletContextHandler, ServletHandler}
import org.eclipse.jetty.util.security.{Constraint, Credential}
import org.eclipse.jetty.util.ssl.SslContextFactory
import org.eclipse.jetty.util.thread.QueuedThreadPool
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.IContext
import org.thymeleaf.templateresolver.ServletContextTemplateResolver

import scala.collection.mutable.ArrayBuffer

object WebServer {


  var threadPool: Option[QueuedThreadPool] = Option(null)
  var server: Option[Server] = Option(null)
  var connector: Option[ServerConnector] = Option(null)
  var https: Option[ServerConnector] = Option(null)
  var handlers: ArrayBuffer[Handler] = ArrayBuffer[Handler]()
  var servletContext: Option[ServletContextHandler] = Option(null)
  var restErrorHandler: Option[(HttpServletRequest, HttpServletResponse, Exception) => JSONClass] = Option(null)
  var loginService: Option[LoginService] = Option(null)
  var securityHandler: Option[ConstraintSecurityHandler] = Option(null)
  var sessionHandler: Option[SessionHandler] = Option(null)
  var localeLangs: Option[Map[String, String]] = Option(null)
  var localeInterceptor: Option[(HttpServletRequest, HttpServletResponse) => String] = Option(null)
  var templateEngines: Option[Map[String, TemplateEngine]] =  Option(null)
  var templateEngine: Option[TemplateEngine] = Option(null)
  var resolvers: Option[Map[String, MessageResolver]] = Option(null)

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
    if (https.isDefined && connector.isDefined) {
      server.get.setConnectors(Array[Connector](connector.get, https.get))
    } else {
      if (connector.isDefined) {
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

  def loginService(f: (String) => Option[User]) {

    object service extends LoginService {

      var identityService: IdentityService = new DefaultIdentityService

      override def setIdentityService(i: IdentityService) = {
        identityService = i
      }

      override def getIdentityService: IdentityService = {
        identityService
      }

      override def getName: String = {
        "Custom"
      }

      override def validate(user: UserIdentity): Boolean = {
        val u = f.apply(user.getUserPrincipal.getName)
        u.isDefined
      }

      override def logout(user: UserIdentity) = {}

      override def login(username: String, credentials: scala.Any, request: ServletRequest): UserIdentity = {
        val user = f.apply(username)
        if (user.isDefined) {
          val pwd = user.get.password
          val roles = user.get.roles
          val userPrincipal = new KnownUser(username, Credential.getCredential(pwd))
          val subject = new Subject
          subject.getPrincipals.add(userPrincipal)
          subject.getPrivateCredentials.add(Credential.getCredential(credentials.toString))

          if (roles.isDefined) {
            roles.get.foreach(r => {
              subject.getPrincipals.add(new RolePrincipal(r))
            })
          }
          subject.setReadOnly
          val identity: UserIdentity = identityService.newUserIdentity(subject, userPrincipal, roles.get)
          val principal = identity.getUserPrincipal.asInstanceOf[UserPrincipal]
          if (principal.authenticate(credentials)) {
            return identity
          }
        }
        null
      }
    }
    loginService = Option(service)
  }


  def securityPages(loginPage: String, loginErrorPage: String, logoutPage: String) = {
    securityHandler = Option(new ConstraintSecurityHandler)
    securityHandler.get.setLoginService(loginService.get)
    val authenticator: FormAuthenticator = new FormAuthenticator(loginPage, loginErrorPage, false)
    securityHandler.get.setAuthenticator(authenticator)
    servletContext = Option(new ServletContextHandler(ServletContextHandler.SESSIONS | ServletContextHandler.SECURITY))
    servletContext.get.setSecurityHandler(securityHandler.get)
    if (sessionHandler.isDefined)
      servletContext.get.setSessionHandler(sessionHandler.get)

    servletContext.get.addServlet(new ServletHolder(new DefaultServlet() {
      protected override def doGet(request: HttpServletRequest, response: HttpServletResponse) {
        request.getSession.invalidate
        response.sendRedirect(response.encodeRedirectURL(loginPage))
      }
    }), logoutPage)

    handlers += servletContext.get
  }


  def addAllowRule(path: String) = {
    val constraint: Constraint = new Constraint
    constraint.setName(Constraint.__FORM_AUTH)

    constraint.setAuthenticate(false)

    val constraintMapping: ConstraintMapping = new ConstraintMapping
    constraintMapping.setConstraint(constraint)
    constraintMapping.setPathSpec(path)
    securityHandler.get.addConstraintMapping(constraintMapping)
  }

  def addRestrictRule(path: String, roles: Array[String]) = {
    val constraint: Constraint = new Constraint
    constraint.setName(Constraint.__FORM_AUTH)

    constraint.setRoles(roles)
    constraint.setAuthenticate(true)

    val constraintMapping: ConstraintMapping = new ConstraintMapping
    constraintMapping.setConstraint(constraint)
    constraintMapping.setPathSpec(path)

    securityHandler.get.addConstraintMapping(constraintMapping)
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
            if (restErrorHandler.isDefined) {
              response.setStatus(HttpServletResponse.SC_BAD_REQUEST)
              response.getWriter().print(JSON.toJson(restErrorHandler.get.apply(request, response, e)))
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

  def addController(page: String, f: (HttpServletRequest, HttpServletResponse, collection.mutable.Map[String, AnyRef]) => String) = {

    object StringServlet extends HttpServlet {
      override def doGet(request: HttpServletRequest, response: HttpServletResponse) = {
        var model = collection.mutable.Map[String, AnyRef]()
        val action = f.apply(request, response,model)

        response.setHeader("Cache-Control", "no-store")
        response.setHeader("Pragma", "no-cache")
        response.setDateHeader("Expires", 0)
        if (action.startsWith("redirect:")) {
          response.sendRedirect(response.encodeRedirectURL(action.substring(9)))
        } else {
          var javaModel =  new java.util.HashMap[String,AnyRef] ()
          model.foreach( m => {
            javaModel.put(m._1, m._2)
          })
          val context: IContext = new ControllerContext(javaModel, request.getServletContext)
          var engine: TemplateEngine = null
          if(templateEngines.isDefined) {
            engine = templateEngines.get.get(localeInterceptor.get.apply(request,response)).get
          } else {
            engine = templateEngine.get
          }
          val ret = engine.process(action, context)
          response.setContentType("text/html; charset=utf-8")
          response.setCharacterEncoding("UTF-8")
          response.setStatus(HttpServletResponse.SC_OK)
          val out = response.getWriter()
          out.write(ret)
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
    addServlet(StringServlet, page)
  }

  def locale(langs: Map[String, String], interceptor: (HttpServletRequest, HttpServletResponse) => String) = {
    localeLangs = Option(langs)
    localeInterceptor = Option(interceptor)
  }

  def templates(folder: String, cacheTTL: Double) = {

    if(localeLangs.isDefined) {
      var engines = collection.mutable.Map[String, TemplateEngine]()
      var tmpResolvers = collection.mutable.Map[String, MessageResolver]()
      localeLangs.get.foreach((lang) => {
        val templateResolver: ServletContextTemplateResolver = new ServletContextTemplateResolver
        templateResolver.setTemplateMode("LEGACYHTML5")
        templateResolver.setPrefix("/")
        templateResolver.setSuffix(".html")
        templateResolver.setCacheTTLMs((cacheTTL * 1000).toLong)
        templateResolver.setCharacterEncoding("UTF-8")
        val engine: TemplateEngine = new TemplateEngine
        engine.setTemplateResolver(templateResolver)
        val localeFile: String = lang._2
        val resolver: MessageResolver = new MessageResolver(localeFile)
        engine.addMessageResolver(resolver)
        tmpResolvers += lang._1 -> resolver
        engine.addDialect(new LayoutDialect)
        engines += lang._1 -> engine
      })
      templateEngines = Option(engines.toMap)
      resolvers = Option(tmpResolvers.toMap)
    } else {
      val templateResolver: ServletContextTemplateResolver = new ServletContextTemplateResolver
      templateResolver.setTemplateMode("LEGACYHTML5")
      templateResolver.setPrefix("/")
      templateResolver.setSuffix(".html")
      templateResolver.setCacheTTLMs((cacheTTL * 1000).toLong)
      templateResolver.setCharacterEncoding("UTF-8")
      val engine: TemplateEngine = new TemplateEngine
      engine.setTemplateResolver(templateResolver)
      templateEngine=Option(engine)
    }
    servletContext.get.setResourceBase(folder)
  }


}


