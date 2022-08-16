package scalax.patch.adapter.collections

import scalax.patch._
import scala.collection.Factory

trait ScalaVersionSpecificKeyedCollectionAdapter {

  implicit def forMap[F[A, B] <: Map[A, B], K, V](implicit
    compV: PatchMaker[V],
    cc: Factory[(K, V), F[K, V]]
  ): KeyedCollectionAdapter[F, K, V] = new KeyedCollectionAdapter[F, K, V] {

    override def applyPatch(coll: F[K, V], key: K, patch: Patch[V]): F[K, V] = patch match {
      case Patch.SetValue(v)   => cc fromSpecific coll.updated(key, v)
      case Patch.UnsetValue(_) => cc fromSpecific { coll - key }
      case patch               => cc fromSpecific coll.updated(key, patch(coll(key)))
    }

    override def diff(left: F[K, V], right: F[K, V]): Map[K, Patch[V]] = {
      val keysToAdd    = right.keySet diff left.keySet
      val keysToRemove = left.keySet diff right.keySet
      var updated      = (right.keySet intersect left.keySet).foldLeft[Map[K, Patch[V]]](Map.empty) {
        case (agg, commonKey) =>
          val l = left(commonKey)
          val r = right(commonKey)
          val p = compV.make(l, r)
          if (p.isOpaque) agg else agg.updated(commonKey, p)
      }
      updated = updated ++ keysToAdd.map(k => (k, Patch.SetValue(right(k))))
      updated = updated ++ keysToRemove.map(k => (k, Patch.UnsetValue(left(k))))

      updated
    }

    override def valuePatchMaker: PatchMaker[V] = compV
  }
}
