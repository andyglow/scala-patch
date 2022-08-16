package scalax.patch.adapter.collections

import scalax.patch._
import scala.collection.Factory

trait ScalaVersionSpecificIndexedCollectionAdapter {

  implicit def forIndexedSeq[F[X] <: IndexedSeq[X], T](implicit
    compT: PatchMaker[T],
    cc: Factory[T, F[T]]
  ): IndexedCollectionAdapter[F, T] = new IndexedCollectionAdapter[F, T] {

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
      val indicesToAdd    = (left.length until right.length).toSet
      val indicesToRemove = (right.length until left.length).toSet

      var updates = (0 until Math.min(left.length, right.length)).foldLeft[Map[Int, Patch[T]]](Map.empty) {
        case (acc, i) =>
          val r = right(i)
          val l = left(i)
          val p = compT.make(l, r)

          if (p != Patch.Empty)
            acc.updated(i, p)
          else
            acc

      }
      updates = updates ++ indicesToAdd.map(i => (i, Patch.SetValue(right(i))))
      updates = updates ++ indicesToRemove.map(i => (i, Patch.UnsetValue(left(i))))

      (right.length - left.length, updates)
    }

    override def resize(coll: F[T], sizeDelta: Int): F[T] = {
      if (sizeDelta < 0) {
        cc fromSpecific coll.dropRight(-sizeDelta)
      } else if (sizeDelta > 0) {
        (0 until sizeDelta).foldLeft(coll) { case (coll, _) =>
          cc fromSpecific coll.appended(null.asInstanceOf[T])
        }
      } else
        coll
    }

    override def elementPatchMaker: PatchMaker[T] = compT
  }
}
