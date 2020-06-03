package scalax.patch.adapter.collections

import scalax.patch._

import scala.collection.Factory


sealed trait IndexedCollectionAdapter[F[_], V] { self =>

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

object IndexedCollectionAdapter {

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
          agg updated (i, Patch.SetValue(v))
        } else {
          val ov = left(i)
          val delta = compT.make(ov, v)
          if (delta != Patch.Empty)
            agg updated (i, delta)
          else
            agg
        }
      }

      (right.length - left.length, deltas)
    }

    override def resize(coll: Array[T], sizeDelta: Int): Array[T] = Array.copyOf(coll, coll.length + sizeDelta)
  }

  implicit def forIndexedSeq[F[X] <: IndexedSeq[X], T](implicit compT: PatchMaker[T], cc: Factory[T, F[T]]): IndexedCollectionAdapter[F, T] = new IndexedCollectionAdapter[F, T] {

    override def applyPatch(coll: F[T], index: Int, patch: Patch[T]): F[T] = {
      var res = coll
      if (index < coll.length) {
        val v = coll(index)
        res = cc fromSpecific res.updated(index, patch(v))
      }

      res
    }

    def size(x: F[T]): Int = x.length

    override def diff(left: F[T], right: F[T]): (Int, Map[Int, Patch[T]]) = {
      val indicesToAdd = (left.length until right.length).toSet
      val indicesToRemove = (right.length until left.length).toSet

      var updates = (0 until Math.min(left.length, right.length)).foldLeft[Map[Int, Patch[T]]](Map.empty) { case (agg, i) =>
        val r = right(i)
        val l = left(i)
        val p = compT.make(l, r)

        if (p != Patch.Empty)
          agg updated (i, p)
        else
          agg

      }
      updates = updates ++ indicesToAdd.map(i => (i, Patch.SetValue(right(i))))
      updates = updates ++ indicesToRemove.map(i => (i, Patch.UnsetValue(left(i))))

      (right.length - left.length, updates)
    }

    override def resize(coll: F[T], sizeDelta: Int): F[T] = {
      if (sizeDelta < 0) {
        cc fromSpecific coll.dropRight(- sizeDelta)
      } else if (sizeDelta > 0) {
        (0 until sizeDelta).foldLeft(coll) {
          case (coll, _) => cc fromSpecific coll.appended(null.asInstanceOf[T])
        }
      } else
        coll
    }
  }
}