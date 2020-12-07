package scalax.patch.adapter.collections

import scalax.ScalaVersionSpecificUtils._
import scalax.patch._


trait IndexedCollectionAdapter[F[_], V] { self =>

  def applyPatch(coll: F[V], index: Int, patch: Patch[V]): F[V]

  def resize(coll: F[V], sizeDelta: Int): F[V]

  def size(coll: F[V]): Int

  def diff(left: F[V], right: F[V]): (Int, Map[Int, Patch[V]])

  class IndexedOps(coll: F[V]) {

    def updatedWith(delta: Map[Int, Patch[V]]): F[V] = delta.foldLeft(coll) {
      case (coll, (key, patch)) => self.applyPatch(coll, key, patch)
    }

    def resized(delta: Int): F[V] = self.resize(coll, delta)

    def size: Int = self.size(coll)
  }

  implicit def mkIndexedOps(coll: F[V]): IndexedOps = new IndexedOps(coll)

}

object IndexedCollectionAdapter extends ScalaVersionSpecificIndexedCollectionAdapter {

  implicit def forArray[T](implicit compT: PatchMaker[T]): IndexedCollectionAdapter[Array, T] = new IndexedCollectionAdapter[Array, T] {

    override def applyPatch(coll: Array[T], index: Int, patch: Patch[T]): Array[T] = {
      if (index < coll.length) {
        val v = coll(index)
        coll.update(index, patch(v))
      }

      coll
    }

    def size(x: Array[T]): Int = x.length

    override def diff(left: Array[T], right: Array[T]): (Int, Map[Int, Patch[T]]) = {
      val deltas = right.zipWithIndex.foldLeft[Map[Int, Patch[T]]](Map.empty) { case (agg, (v, i)) =>
        if (i >= left.length) {
          agg.updated(i, Patch.SetValue(v))
        } else {
          val ov = left(i)
          val delta = compT.make(ov, v)
          if (delta != Patch.Empty)
            agg.updated(i, delta)
          else
            agg
        }
      }

      (right.length - left.length, deltas)
    }

    override def resize(coll: Array[T], sizeDelta: Int): Array[T] = copyArray(coll, sizeDelta)
  }
}