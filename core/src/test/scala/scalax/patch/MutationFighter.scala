package scalax.patch

import scalax.ScalaVersionSpecificUtils._

trait MutationFighter[T] {

  def copy(x: T): T
}

object MutationFighter {

  def copy[T](x: T)(implicit mf: MutationFighter[T]): T = mf.copy(x)

  implicit def forArray[T]: MutationFighter[Array[T]] = new MutationFighter[Array[T]] {
    override def copy(x: Array[T]): Array[T] = copyArray(x, 0)
  }

  implicit def forTheRestImmutableTypes[T]: MutationFighter[T] = new MutationFighter[T] {
    override def copy(x: T): T = x
  }
}
