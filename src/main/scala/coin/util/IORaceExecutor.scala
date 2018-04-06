package coin.util

import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

import cats.effect.IO
import cats.implicits._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{CancellationException, ExecutionContext}

class IORaceExecutor[T,R](producer: () => T, consumer: T => Option[R]) {
  private val operations = new AtomicInteger(0)

  val rateCounter = new AtomicInteger(0)

  val cores = Runtime.getRuntime.availableProcessors

  val tp = Executors.newFixedThreadPool(cores + 1)
  val ec = ExecutionContext.fromExecutor(tp)

  val canceledError = new CancellationException("CANCELED")


  private def parallelIOs[A](count: Int, ec: ExecutionContext, task: IO[A]) = {
    for (_ <- 0 to count) yield IO.shift(ec) *> task
  }

  private def raceN[A](tasks: Seq[IO[A]]): IO[A] = tasks
    .foldLeft[IO[A]](IO.never)((acc, e) =>
      IO.race[A, A](acc, e) map {
        case Left(a) => a
        case Right(a) => a
      })

  private def rate: IO[Unit] = {
    for {
      _ <- IO.sleep(1.second)
      _ <- IO(rateCounter.set(0))
      _ <- rate
    } yield ()
  }

  private def calculate: IO[Option[R]] = IO (
    consumer(producer())
  )

  private def wrap: IO[R] = {
    calculate flatMap {
      case Some(res) =>
        IO.pure(res)

      case None =>
        rateCounter.incrementAndGet()

        val ops = operations.incrementAndGet()

        if (ops % 10000 == 0)
          IO.cancelBoundary *> wrap
        else wrap
    }
  }

  private def releaseResources = IO[Unit] (tp.shutdown())

  def run: IO[R] = {
    val computeNonce = raceN(parallelIOs(cores, ec, wrap))

    def extractResult: Either[R, Unit] => IO[R] = {
      case Left(value) =>
        IO.pure(value)
      case _ =>
        IO.raiseError(new IllegalStateException("Should not be here"))
    }

    val recoverCancelation: PartialFunction[Throwable, IO[R]] = {
      case `canceledError` =>
        for {
          _ <- releaseResources
          res <- IO.raiseError(canceledError)
        } yield res
    }

    val res =
      for {
        wrapped <-IO.race(computeNonce, rate)
        res <- extractResult(wrapped)
        _ <- releaseResources
      } yield res

    res.onCancelRaiseError(canceledError).recoverWith(recoverCancelation)
  }
}
