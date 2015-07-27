package com.igumnov.scala

import com.igumnov.common.{File}
import java.io.{File => JavaFile}
import java.util.stream.{Stream => JavaStream}

import scala.collection.mutable


object FileOps {
  def readAllToString(fileName: String): String = {
    File.readAllToString(fileName)
  }

  def writeString(str: String, fileName: String) {
    File.writeString(str, fileName)
  }

  def appendLine(line: String, filename: String) {
    File.appendLine(line, filename)
  }

  def readLines(fileName: String): List[String] = {
    val lines: JavaStream[String] = File.readLines(fileName)
    val ret = mutable.MutableList[String]()
    lines.toArray().foreach(l => ret += l.asInstanceOf[String])
    ret.toList
  }

  def removeFileIfExists(fileName: String): Boolean = {
    val f = new JavaFile(fileName)
    if (f.exists()) {
      f.delete()
    } else {
      false
    }
  }

}
