package coin.util

import java.lang.System.currentTimeMillis
import java.util.concurrent.atomic.{AtomicInteger, AtomicLong}
import java.util.concurrent.{Callable, TimeUnit}

import cats.effect.Sync

object NoResult extends Exception

class RaceExecutor[A,R](producer: () => A, consumer: A => Option[R]) {

  import java.util.concurrent.Executors

  val numThreads = Runtime.getRuntime.availableProcessors + 1
  val executor = Executors.newFixedThreadPool(numThreads)

  private val c = new AtomicInteger(0)
  private val time = new AtomicLong(0)

  def apply[F[_]](implicit F: Sync[F]): F[R] = {
    val computation = F.delay {

      val futures =
        for (n <- 0 to numThreads)
          yield executor.submit(callable)


      executor.awaitTermination(256, TimeUnit.DAYS)

      futures.map(_.get()).collectFirst {
        case Some(value) => value
      }
    }

    F.flatMap(computation) {
      case Some(value) => F.pure(value)
      case None => F.raiseError(NoResult)
    }
  }

  private def callable: Callable[Option[R]] = () => {
    var result: Option[R] = None

    while (!executor.isShutdown) {
      count()
      val value = consumer(producer())

      if (value.isDefined) {
        result = value
        executor.shutdown()
      }
    }

    result
  }

  private def count(): Unit = {
    c.incrementAndGet()

    val currTime = currentTimeMillis()
    if (currTime - time.get() > 1000) {
      print(s"\rRate: $c")
      time.set(currTime)
      c.set(0)
    }
  }
}
