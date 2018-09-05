package coin

import java.util.concurrent.Executors

import cats.effect._
import cats.effect.concurrent.Ref
import cats.syntax.applicative._
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.instances.list._
import coin.util.{Cancelable, Race}

import scala.concurrent.ExecutionContext

trait Executor[F[_]] {
  def run[A](gen: F[Option[A]]): F[A]
}

class ParallelExecutor[F[_]: Async: Cancelable: Race](timer: Timer[F], rate: Ref[F, Long], ops: Ref[F, Long]) extends Executor[F] {
  val cores = Runtime.getRuntime.availableProcessors
  val executor = Executors.newFixedThreadPool(cores)
  val ec = ExecutionContext.fromExecutor(executor)

  private def parallellF[A](count: Int, task: F[A]) = {
    for (_ <- 0 to count) yield Async.shift(ec) *> task
  }

  private def wrap[A](calculate: F[Option[A]]): F[A] = {
    def loop: F[A] = {
      calculate >>= {
        case Some(res) =>
          res.pure[F]

        case None =>
          for {
            _   <- rate.update(_ + 1)
            _   <- ops.update(_ + 1)
            o   <- ops.get
            res <-  if (o % 10000 == 0) Cancelable[F].cancelBoundary *> loop
                  else loop
          } yield res
      }
    }

    loop
  }

  def rateEff: F[Long] =
    for {
      res <- rate.getAndSet(0)
    } yield res

  override def run[A](gen: F[Option[A]]): F[A] = {
    val wrapped = wrap(gen)
    val paralleled = parallellF(cores, wrapped).toList
    val computeNonce = Race[F].raceN(paralleled)

    Bracket[F,Throwable].guarantee(computeNonce)(Async[F].delay(executor.shutdown()))
  }
}

object ParallelExecutor {
  def apply[F[_]: Async: Cancelable: Race](timer: Timer[F]): F[ParallelExecutor[F]] =
    for {
      rate  <- Ref.of[F, Long](0)
      ops   <- Ref.of[F, Long](0)
    } yield new ParallelExecutor[F](timer, rate, ops)
}
