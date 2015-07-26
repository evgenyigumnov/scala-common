package igumnov.common

import java.io.File

object FolderOps {

  def createIfNotExists (folderName: String): Boolean = {
    val f = new File(folderName)
    if(!f.exists()) {
      f.mkdir()
      true
    } else {
      false
    }
  }

}
