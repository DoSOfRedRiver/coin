package coin.util

import cats.Foldable
import cats.effect.{Async, IO}
import cats.syntax.functor._
import cats.syntax.foldable._
import coin.util.instances.RaceInstances


trait Race[F[_]] {
  def race[A,B](lh: F[A], rh: F[B]): F[Either[A, B]]

  def raceN[A, G[_]](tasks: G[F[A]])(implicit async: Async[F], ev2: Foldable[G]): F[A] = {
    tasks.foldLeft[F[A]](async.never)((acc, e) =>
      race[A, A](acc, e) map (_.merge))
  }
}

object Race extends RaceInstances {
  def apply[F[_]](implicit ev: Race[F]): Race[F] = ev
}
