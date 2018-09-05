package coin

import cats.Monad
import cats.effect.Sync
import cats.effect.concurrent.Ref
import cats.syntax.flatMap._
import cats.syntax.functor._
import monocle.macros.GenLens

trait Pow[F[_], T] {
  def gen: F[Block[T]]
  def tryBlock: F[Option[Block[T]]]
}

class ProofOfWork[F[_]: Monad, T](block: Block[T], seed: Ref[F, Long], prover: Prover[F]) extends Pow[F, T] {
  val lens = GenLens[Block[T]](_.header.nonce)

  def gen: F[Block[T]] =
    for {
      _     <- seed.update(_ + 1)
      nonce <- seed.get
    } yield lens.set(nonce)(block)

  def tryBlock: F[Option[Block[T]]] =
    for {
      newBlock  <- gen
      res       <- prover.calculate(newBlock)
    } yield res
}

object ProofOfWork {
  def apply[F[_]: Sync, T](block: Block[T], prover: Prover[F]): F[ProofOfWork[F, T]] =
    for {
      seed <- Ref[F].of(0L)
    } yield new ProofOfWork(block, seed, prover)
}