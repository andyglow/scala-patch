package scalax.patch.adapter.collections

import scalax.patch._

import scala.collection._


trait OrderedCollectionAdapter[F[_], T] {
  import OrderedCollectionAdapter._

  def apply(coll: F[T], diff: Diff[T]): F[T]
  def diff(left: F[T], right: F[T]): Diff[T]

  class OrderedOps(coll: F[T]) {
    def computeDiff(another: F[T]): Diff[T] = diff(coll, another)
    def applyDiff(diff: Diff[T]): F[T] = apply(coll, diff)
  }

  implicit def mkOrderedOps(coll: F[T]): OrderedOps = new OrderedOps(coll)
}

object OrderedCollectionAdapter extends ScalaVersionSpecificOrderedCollectionAdapter {

  case class Diff[T](events: List[Diff.Evt[T]]) {
    import Diff._
    import Evt._

    def inverted: Diff[T] = Diff(events map {
      case Skip(n)      => Skip[T](n)
      case Insert(xs)   => Drop(xs)
      case Drop(xs)     => Insert(xs)
      case Upgrade(xs)  => Upgrade(xs map { _.inverted })
    })

    def +:(e: Evt[T]): Diff[T] = (events.headOption, e) match {
      case (Some(Skip(n)), Skip(en))        => Diff(events = Skip[T](n + en) +: events.drop(1))
      case (Some(Upgrade(s)), Upgrade(es))  => Diff(events = Upgrade(es ++ s) +: events.drop(1))
      case _                                => Diff(events = e +: events)
    }
    def ++(d: Diff[T]): Diff[T] = Diff(events = events ++ d.events)

    override def toString: String = s"Diff(${events mkString ","})"
  }
  object Diff {
    def empty[T] = Diff[T](Nil)
    def apply[T](x: Evt[T]): Diff[T] = Diff(List(x))
    def compute[T: PatchMaker](left: LinearSeq[T], right: LinearSeq[T]): Diff[T] = {
      import Evt._
      val l = left.headOption
      val r = right.headOption

      (l, r) match {
        case (None, None)                 => Diff.empty
        case (None, _)                    => Diff(Insert(right.toList))
        case (_, None)                    => Diff(Drop(left.toList))
        case (Some(l), Some(r)) if l == r => Skip[T](1) +: compute(left.tail, right.tail)
        case (Some(l), Some(r))           => Upgrade(l, r) +: compute(left.tail, right.tail)
      }
    }
    sealed trait Evt[T]
    object Evt {
      case class Skip[T](n: Int) extends Evt[T]
      case class Insert[T](elements: List[T]) extends Evt[T] {
        override def toString: String = s"Insert(${elements mkString ","})"
      }
      case class Upgrade[T](xs: List[Patch[T]]) extends Evt[T] {
        override def toString: String = s"Upgrade(${xs mkString ","})"
      }
      object Upgrade {
        def apply[T: PatchMaker](l: T, r: T): Upgrade[T] = Upgrade(List(Patch.make(l, r)))
      }
      case class Drop[T](elements: List[T]) extends Evt[T] {
        override def toString: String = s"Drop(${elements mkString ","})"
      }
    }
  }
}