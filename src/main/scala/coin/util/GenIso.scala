package coin.util

import cats.{Functor, ~>}
import coin.util.instances.ConvertibleInstances
import monocle.PIso

import scala.language.higherKinds

object GenIso {
  trait Convertible[A,B] {
    def apply(a: A): B
  }

  object Convertible extends ConvertibleInstances

  class GenIso[S,D] {
    def apply[G[_],F[_]](N: G ~> F)(implicit
      C1: Convertible[S,D],
      C2: Convertible[D,S],
      G: Functor[G]
    ): PIso[S,F[S],D,G[D]] = PIso[S,F[S],D,G[D]](C1(_))(g => N(G.map(g)(C2(_))))
  }

  def genIso[S,D] = new GenIso[S,D]

  def genIso[S,D,F[_]](implicit
    C1: Convertible[S,D],
    C2: Convertible[D,S],
    G: Functor[F]
  ): PIso[S,F[S],D,F[D]] = PIso[S,F[S],D,F[D]](C1(_))(g => G.map(g)(C2(_)))
}
