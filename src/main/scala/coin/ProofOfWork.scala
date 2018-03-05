package coin

import cats.effect.Sync
import cats.implicits._
import coin.util.RaceExecutor
import monocle.macros.GenLens

import scala.annotation.tailrec

object ProofOfWork {
  val complexity = 1

  def proof[T](block: Block[T], from: Long = 0): Block[T] = {
    val lens = GenLens[Block[T]](_.header.nonce)

    @tailrec
    def rec(nonce: Long): Block[T] = {
      val newBlock = lens.set(nonce)(block)

      calculate(newBlock) match {
        case Some(value) => value
        case None => rec(nonce + 1)
      }
    }

    rec(from)
  }

  def calculate[T](block: Block[T]): Option[Block[T]] = {
    val ints = block.hash.numbers.takeRight(complexity)
    if (ints.sum === 0) Some(block)
    else None
  }

  def proofMultithreaded[T,F[_]: Sync](block: Block[T]): F[Block[T]] = {
    val lens = GenLens[Block[T]](_.header.nonce)

    var nonce = 0

    val mutator = () => {
      nonce = nonce + 1
      lens.set(nonce)(block)
    }

    val executor = new RaceExecutor[Block[T],Block[T]](mutator, calculate)
    executor[F]
  }
}