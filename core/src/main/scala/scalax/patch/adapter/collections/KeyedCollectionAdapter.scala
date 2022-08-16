package scalax.patch.adapter.collections

import scalax.patch._


trait KeyedCollectionAdapter[F[_, _], K, V] {

  def valuePatchMaker: PatchMaker[V]

  def applyPatch(coll: F[K, V], key: K, patch: Patch[V]): F[K, V]

  def diff(left: F[K, V], right: F[K, V]): Map[K, Patch[V]]

  class KeyedOps(coll: F[K, V]) {

    def updatedWith(delta: Map[K, Patch[V]]): F[K, V] = delta.foldLeft(coll) {
      case (coll, (key, patch)) => applyPatch(coll, key, patch)
    }
  }

  implicit def mkKeyedOps(coll: F[K, V]): KeyedOps = new KeyedOps(coll)
}

object KeyedCollectionAdapter extends ScalaVersionSpecificKeyedCollectionAdapter