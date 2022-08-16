package scalax.patch.adapter.collections

import scalax.patch._

trait UnorderedCollectionAdapter[F[_], T] {
  import UnorderedCollectionAdapter._

  def apply(coll: F[T], diff: Diff[T]): F[T]
  def diff(left: F[T], right: F[T]): Diff[T]

  class UnorderedOps(coll: F[T]) {
    def computeDiff(another: F[T]): Diff[T] = diff(coll, another)
    def applyDiff(diff: Diff[T]): F[T]      = apply(coll, diff)
  }

  implicit def mkUnorderedOps(coll: F[T]): UnorderedOps = new UnorderedOps(coll)
}

object UnorderedCollectionAdapter extends LowPriorityUnorderedCollectionAdapter {

  final case class Diff[T](events: Seq[Diff.Evt[T]]) {
    import Diff._
    import Evt._

    def isEmpty: Boolean  = events.isEmpty
    def inverted: Diff[T] = Diff(events map {
      case Add(xs)    => Remove(xs)
      case Remove(xs) => Add(xs)
    })
  }
  final object Diff                                  {
    def apply[T](x: Evt[T], xs: Evt[T]*): Diff[T] = Diff({ x +: xs }.filterNot(_.isEmpty))
    sealed trait Evt[T] { def isEmpty: Boolean }
    final object Evt    {
      final case class Add[T](xs: Seq[T])    extends Evt[T] { def isEmpty: Boolean = xs.isEmpty }
      final case class Remove[T](xs: Seq[T]) extends Evt[T] { def isEmpty: Boolean = xs.isEmpty }
    }
  }

  import Diff._
  import Evt._

  implicit def forSet[T: PatchMaker]: UnorderedCollectionAdapter[Set, T] = new UnorderedCollectionAdapter[Set, T] {

    override def apply(coll: Set[T], diff: Diff[T]): Set[T] = diff.events.foldLeft(coll) {
      case (coll, Add(xs))    => coll ++ xs
      case (coll, Remove(xs)) => val index = xs.toSet; coll.filterNot(index.apply)
    }

    override def diff(left: Set[T], right: Set[T]): Diff[T] = {

      def toAdd    = (right diff left).toSeq
      def toRemove = (left diff right).toSeq

      Diff(Add(toAdd), Remove(toRemove))
    }
  }
}
