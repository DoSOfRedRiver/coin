package coin

import cats.implicits._
import monocle.macros.GenLens

import scala.annotation.tailrec

object ProofOfWork {
  val complexity = 1

  def proof[T](block: Block[T]): Block[T] = {
    val lens = GenLens[Block[T]](_.header.nonce)

    def calculate(block: Block[T])(nonce: Long) = {
      val newBlock = lens.set(nonce)(block)
      val ints = block.hash.numbers.takeRight(complexity)
      if (ints.sum === 0) Some(newBlock)
      else None
    }

    @tailrec
    def rec(nonce: Long): Block[T] = {
      val r = calculate(block)(nonce)
      if (r.isDefined) r.get
      else rec(nonce + 1)
    }

    rec(0)
  }
}
