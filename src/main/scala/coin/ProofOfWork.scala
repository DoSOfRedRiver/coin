package coin

import cats.implicits._

object ProofOfWork {
  val complexity = 1

  def proof[T](block: Block[T]): Block[T] = {
    def rec(nonce: Long): Block[T] = {
      val r = f(block)(nonce)
      if (r.isDefined) r.get
      else rec(nonce + 1)
    }

    rec(0)
  }


  private def f[T](block: Block[T])(nonce: Long) = {
    val newBlock = block.copy(nonce = nonce)
    val ints = block.hash.numbers.takeRight(complexity)
    if (ints.sum === 0) Some(newBlock)
    else None
  }
}
