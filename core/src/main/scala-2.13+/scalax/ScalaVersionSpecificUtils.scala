package scalax

object ScalaVersionSpecificUtils {

  @inline def getOrElse[L, R, RR >: R](x: Either[L, R], default: => RR): RR = x.getOrElse(default)

  @inline def mapValues[K, V, VV](x: Map[K, V], fn: V => VV): Map[K, VV] = x.view.mapValues(fn).toMap

  @inline def copyArray[T](source: Array[T], sizeDelta: Int): Array[T] = Array.copyOf(source, source.length + sizeDelta)
}
