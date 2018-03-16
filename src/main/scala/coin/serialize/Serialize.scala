package coin.serialize

import cats.data.ReaderT
import cats.effect.Effect
import coin.util.instances.SerializeInstances

import scala.language.higherKinds

trait Serialize[S,A] {
  def write[F[_]: Effect](a: => A): ReaderT[F,S,Unit]
  def read[F[_]: Effect]: ReaderT[F,S,A]
}

object Serialize extends SerializeInstances
