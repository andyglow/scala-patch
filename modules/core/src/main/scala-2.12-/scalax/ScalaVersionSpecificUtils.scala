package scalax

import scala.reflect.ClassTag

object ScalaVersionSpecificUtils {

  @inline def getOrElse[L, R, RR >: R](x: Either[L, R], default: => RR): RR = x.right.getOrElse(default)

  @inline def mapValues[K, V, VV](x: Map[K, V], fn: V => VV): Map[K, VV] = x.mapValues(fn)

  @inline def copyArray[T](source: Array[T], sizeDelta: Int): Array[T] = {
    implicit def elemTag: ClassTag[T] = source.elemTag

    sizeDelta match {
      // same
      case 0                  =>
        val target = Array.ofDim[T](source.length)
        Array.copy(source, 0, target, 0, source.length)
        target
      // truncate
      case delta if delta < 0 =>
        val target = Array.ofDim[T](source.length + delta)
        Array.copy(source, 0, target, 0, source.length + delta)
        target
      // pad
      case delta if delta > 0 =>
        val target = Array.ofDim[T](source.length + delta)
        Array.copy(source, 0, target, 0, source.length)
        target
    }
  }
}
