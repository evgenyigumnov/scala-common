# scala-common
Common Scala Library

    libraryDependencies += "com.igumnov.scala" % "scala-common_2.11" % "0.2"

JSON usage

    // File: SampleEntity.scala
    import com.igumnov.scala.JSON
    import com.igumnov.scala.JSON._
    class SampleEntity extends JSON [SampleEntity]{
      var name: String = _
      var value: Int = _
    }
    object SampleEntity extends SampleEntity

    // File: usage.scala
    import com.igumnov.scala._
    val jsonString = """{ "name": "JSON source", "value": 1 }"""
    val myObject = SampleEntity.fromJson(jsonString)
    val myObject2 = new SampleEntity()
    myObject2.name="Some name"
    myObject2.value=100
    val jsonString2 = myObject2.toJson
    val jsonStringArray =  toJson(Array(myObject,myObject2))
    val exampleArray = fromJson[Array[SampleEntity]](jsonStringArray)


WebServer usage

    import com.igumnov.scala.WebServer._
    import com.igumnov.scala.webserver.User

    setPoolSize(1, 20)
    init("localhost", 8888)
    https("localhost", 8889, "src/test/resources/key.jks", "storepwd", "keypwd")

    loginService((name) => {
      if (name == "admin") {
        Option(new User("admin", "CRYPT:adpexzg3FUZAk", Option(Array[String]("user"))))
      } else {
        Option(null)
      }
    })

    securityPages("/login", "/login", "/logout")

    addStringController("/login", (rq, rs) => {
      "<form method='POST' action='/j_security_check'>" + "<input type='text' name='j_username'/>" +
        "<input type='password' name='j_password'/>" + "<input type='submit' value='Login'/></form>"
    })


    addRestController[SampleEntity]("/rest", (rq, rs, obj) => {
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


    addRestErrorHandler((rq: HttpServletRequest, rs: HttpServletResponse, e: Exception) => {
      val ret = new ErrorEntity()
      ret.message = e.getMessage
      ret
    })

    val langs = Map[String, String]("ru" -> "tmp/locale_ru.properties",
      "en" -> "tmp/locale_en.properties")

    locale(langs, (rq,rs)=>{
      "en"
    })

    templates("pages",0)

    start


ORM usage

    // ObjectDTO.scala
    import com.igumnov.scala.orm.Id
    class ObjectDTO {
        @Id(autoIncremental = true)
        var id:Long = _
        var name:String = _
        var salary: Int = _
    }


    // ORMTest.scala
    import com.igumnov.scala.ORM._
    import com.igumnov.scala._


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

      obj.name="b"
      update[ObjectDTO](obj)

      val retList = findAll[ObjectDTO]
      val retList2 = findBy[ObjectDTO]("name=?","b")
      delete(obj)
    }