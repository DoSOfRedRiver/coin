package coin

import cats.effect.Sync
import cats.syntax.applicative._
import cats.syntax.flatMap._

trait Prover[F[_]] {
  def calculate[T](block: Block[T]): F[Option[Block[T]]]
  def complexity: F[Int]
}

class SimpleProver[F[_]: Sync] extends Prover[F] {
  override def calculate[T](block: Block[T]) = complexity >>= { comp =>
    Sync[F].delay {
      val leading = block.hash.hex.take(comp)

      if (leading.forall(_ == '0')) Some(block)
      else None
    }
  }

  override def complexity: F[Int] = 6.pure[F]
}
