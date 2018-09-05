package coin.util

import coin.util.instances.CancelableInstances

trait Cancelable[F[_]] {
  def cancelBoundary: F[Unit]
}

object Cancelable extends CancelableInstances {
  def apply[F[_]: Cancelable] = implicitly[Cancelable[F]]
}
