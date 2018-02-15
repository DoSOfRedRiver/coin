package coin.serialize

import java.io._

sealed trait StreamsStorage {
  def is: InputStream
  def os: OutputStream
}

case class FileStorage(file: File) extends StreamsStorage {
  val os = new FileOutputStream(file)
  val is = new FileInputStream(file)
}

case class MemoryStorage() extends StreamsStorage {
  override val os = new ByteArrayOutputStream()
  override val is = new InputStream {
    var readed = 0
    var bytes = Array[Byte]()

    override def read() = {
      if (readed >= bytes.length) {
        bytes = os.toByteArray
      }

      if (readed >= bytes.length) -1
      else {
        readed = readed + 1
        bytes(readed - 1)
      }
    }
  }
}
