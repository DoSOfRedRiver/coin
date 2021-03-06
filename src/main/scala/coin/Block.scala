package coin

import java.nio.charset.Charset
import java.security.MessageDigest

import coin.util.converters.BytesToHex
import monocle.macros.GenLens

import scala.language.higherKinds


case class Hash(bytes: Seq[Byte]) {
  val hex = BytesToHex.bytesToHex(bytes.toArray)

  override def toString: String = s"Hash($hex)"
}

case class Header(index: Long, prevHash: Hash, nonce: Long, timestamp: Long = System.currentTimeMillis())

case class  Block[T](header: Header, body: T) {
  import Block._

  val bytes = toString.getBytes(Block.UtfCharset)

  lazy val hash: Hash = Hash(digest(bytes))
}

object Block {
  val UtfCharset: Charset = Charset.forName("UTF-8")

  def digest: Array[Byte] => Array[Byte] =
    MessageDigest.getInstance("SHA-256").digest

  def fromBlock[T,A](block: Block[T], body: A): Block[A] = {
    val header = Header(block.header.index + 1, block.hash, nonce = 0)
    Block(header, body)
  }

  case object BlockNotFoundException extends Exception
}