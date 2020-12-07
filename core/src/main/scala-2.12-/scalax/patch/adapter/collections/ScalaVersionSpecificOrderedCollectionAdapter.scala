package scalax.patch.adapter.collections

import scalax.patch._

import scala.collection._
import scala.collection.generic.CanBuildFrom


trait ScalaVersionSpecificOrderedCollectionAdapter {
  import OrderedCollectionAdapter._

  implicit def forLinearSeq[F[X] <: LinearSeq[X], T: PatchMaker](implicit cc: CanBuildFrom[Traversable[T], T, F[T]]): OrderedCollectionAdapter[F, T] = new OrderedCollectionAdapter[F, T] {
    import Diff._
    import Evt._

    override def apply(coll: F[T], diff: Diff[T]): F[T] = {
      var tail: LinearSeq[T] = coll
      val res = mutable.ListBuffer[T]()
      diff.events foreach {
        case Skip(n)        => res ++= tail.take(n); tail = tail.drop(n)
        case Insert(es)     => res ++= es
        case Drop(es)       => tail = tail.drop(es.length)
        case Upgrade(es)    => res ++= es.zip(tail.take(es.length)).map { case (patch, e) => patch(e) }; tail = tail.drop(es.length)
      }

      cc(res).result()
    }

    override def diff(left: F[T], right: F[T]): Diff[T] = Diff.compute(left, right)
  }
}
