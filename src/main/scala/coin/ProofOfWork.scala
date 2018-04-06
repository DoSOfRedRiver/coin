package coin

import java.util.concurrent.atomic.AtomicLong

import cats.effect.{IO, Sync}
import coin.util.{IORaceExecutor, RaceExecutor}
import monocle.macros.GenLens

import scala.annotation.tailrec

object ProofOfWork {
  val complexity = 8

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
    val leading = block.hash.hex.take(complexity)

    if (leading.forall(_ == '0')) Some(block)
    else None
  }

  def proofMultithreaded[T, F[_]: Sync](block: Block[T]): F[Block[T]] = {
    val lens = GenLens[Block[T]](_.header.nonce)

    var nonce = 0

    val mutator = () => {
      nonce = nonce + 1
      lens.set(nonce)(block)
    }

    val executor = new RaceExecutor[Block[T],Block[T]](mutator, calculate)
    executor[F]
  }

  def proofMultithreadedIo[T](block: Block[T]): IO[Block[T]] = {
    val lens = GenLens[Block[T]](_.header.nonce)
    val nonce = new AtomicLong(-1)

    val producer = () => lens.set(nonce.incrementAndGet())(block)

    val ex = new IORaceExecutor[Block[T],Block[T]](producer, calculate)

    ex.run
  }
}