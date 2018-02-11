package coin

import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.security.MessageDigest


case class Hash(bytes: Array[Byte]) {
  val numbers: Array[Long] = {
    val bb = ByteBuffer.wrap(bytes).asLongBuffer()
    val arr = Array.fill[Long](bytes.length / 8)(0)
    bb.get(arr)
    arr
  }

  override def toString: String = s"Hash(${numbers.mkString(" ")})"
}

case class Block[T](index: Int, previousHash: Hash, body: T, nonce: Long, timestamp: Long = System.currentTimeMillis()) {
  import Block.Digest._

  val bytes = toString.getBytes(Block.UtfCharset)

  lazy val hash: Hash = Hash(digest(bytes))
}

object Block {
  val Digest = MessageDigest.getInstance("SHA-256")
  val UtfCharset = Charset.forName("UTF-8")

  def fromBlock[T,A](block: Block[T], body: A): Block[A] = {
    Block(block.index + 1, block.hash, body, 0)
  }
}