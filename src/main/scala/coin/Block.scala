package coin

import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.security.MessageDigest

import scala.language.higherKinds


case class Hash(bytes: Seq[Byte]) {
  val numbers: Array[Int] = {
    val bb = ByteBuffer.wrap(bytes.toArray).asIntBuffer()
    val arr = Array.fill[Int](bytes.length / 4)(0)
    bb.get(arr)
    arr
  }

  override def toString: String = s"Hash(${numbers.mkString(" ")})"
}

case class Header(index: Long, prevHash: Hash, nonce: Long, timestamp: Long = System.currentTimeMillis())

case class  Block[T](header: Header, body: T) {
  import Block._

  val bytes = toString.getBytes(Block.UtfCharset)

  lazy val hash: Hash = Hash(digest(bytes))
}

object Block {
  def digest: Array[Byte] => Array[Byte] =
    MessageDigest.getInstance("SHA-256").digest

  val UtfCharset: Charset = Charset.forName("UTF-8")

  def fromBlock[T,A](block: Block[T], body: A): Block[A] = {
    val header = Header(block.header.index + 1, block.hash, nonce = 0)
    Block(header, body)
  }

  case object BlockNotFoundException extends Exception
}