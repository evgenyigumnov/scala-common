package com.igumnov.scala



import org.scalatest._

class JSONTest extends FlatSpec {


  "Jackson" should "serialase and deserialase json" in {

    val source = """{ "name": "JSON source", "value": 1 }"""

    val caseObject = CaseEntity().fromJson(source)
    assert(caseObject.value == Some(1))

    var myObject = SampleEntity.fromJson(source)
    assert(myObject.value == 1)

    assert(myObject.toJson == "{\"name\":\"JSON source\",\"value\":1}")

    var myObject2 = new SampleEntity()

    myObject2.name="2"
    myObject2.value=2

    var example =  JSON.toJson(Array(myObject,myObject2))

    assert(example == "[{\"name\":\"JSON source\",\"value\":1},{\"name\":\"2\",\"value\":2}]")
    val exampleArray = JSON.fromJson[Array[SampleEntity]](example)
    assert(exampleArray(1).name == "2")

  }
}


