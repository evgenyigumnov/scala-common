# scala-common
Common Scala Library

    libraryDependencies += "com.igumnov.scala" % "scala-common" % "0.1"

Example of usage

    // File: SampleEntity.scala
    import com.igumnov.scala._
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
    val jsonStringArray =  JSON.toJson(Array(myObject,myObject2))
    val exampleArray = JSON.fromJson[Array[SampleEntity]](jsonStringArray)


