package coin

import java.io.Closeable

package object util {
  def withCloseable[T <: Closeable, R](c: T)(f: T => R): R = {
    val r = f(c)
    c.close()
    r
  }
}
