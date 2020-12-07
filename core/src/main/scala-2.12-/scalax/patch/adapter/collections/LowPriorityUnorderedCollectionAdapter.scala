package scalax.patch.adapter.collections

import scalax.patch._

import scala.collection.generic.CanBuildFrom


trait LowPriorityUnorderedCollectionAdapter {
  import UnorderedCollectionAdapter._
  import Diff._
  import Evt._

  implicit def forIterable[F[X] <: Traversable[X], T: PatchMaker](implicit cc: CanBuildFrom[Traversable[T], T, F[T]]): UnorderedCollectionAdapter[F, T] = new UnorderedCollectionAdapter[F, T] {

    override def apply(
      coll: F[T],
      diff: Diff[T]): F[T] = diff.events.foldLeft(coll) {
      case (coll, Add(xs))    => (cc(coll) ++= xs).result()
      case (coll, Remove(xs)) => val index = xs.toSet; cc(coll.filterNot(index.apply)).result()
    }

    override def diff(
      left : F[T],
      right: F[T]): Diff[T] = {
      val ls       = left.toSeq
      val rs       = right.toSeq
      val toAdd    = rs diff ls
      val toRemove = ls diff rs

      Diff(Add(toAdd), Remove(toRemove))
    }
  }
}