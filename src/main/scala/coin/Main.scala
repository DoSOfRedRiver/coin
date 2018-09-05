package coin

import cats.MonadError
import cats.effect._
import cats.syntax.flatMap._
import cats.syntax.functor._
import coin.util._
import monocle.macros.GenLens

import scala.concurrent.duration._

object Main extends IOApp {
  override def run(args: List[String]) = {
    val genesisBlock = Block(Header(0,Hash(Seq.empty), -1522759259, 1520117211495L),"genesis")

    val lens = GenLens[Block[String]](_.header.nonce)
    val block = lens.set(0)(genesisBlock)

    val prover = new SimpleProver[IO]
    val proofOfWork = ProofOfWork(block, prover)


    def printRate[F[_]: Sync](pe: ParallelExecutor[F], timer: Timer[F]): F[Unit] =
      for {
        _     <- timer.sleep(1.second)
        rate  <- pe.rateEff
        _     <- Sync[F].delay(print(s"\rRate: $rate"))
        _     <- printRate(pe, timer)
      } yield ()

    import cats.syntax.either._

    def calculateBlock[F[_]: Async: Race: Cancelable, T](pow: Pow[F, T], timer: Timer[F]) =
      for {
        pe        <- ParallelExecutor(timer)
        res       <- Race[F].race(printRate(pe, timer), pe.run(pow.tryBlock))
        errored = res.leftMap(_ => new IllegalStateException("Should not be here"))
        unwrapped <- MonadError[F, Throwable].fromEither(errored)
      } yield unwrapped

    val program =
      for {
        pow   <- proofOfWork
        block <- calculateBlock(pow, timer)
        _     <- IO(println(s"Block: $block\nHash: ${block.hash}"))
      } yield ()

    program.as(ExitCode.Success)
  }
}
