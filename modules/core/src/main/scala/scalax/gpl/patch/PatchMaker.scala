package scalax.gpl.patch

import scalax.gpl.Exported
import scalax.gpl.patch.PatchMaker.PurePatchMaker
import scalax.gpl.patch.adapter.collections.UnorderedCollectionAdapter
import scalax.gpl.patch.adapter._
import scalax.gpl.patch.adapter.collections._

trait PatchMaker[T] {

  def make(l: T, r: T): Patch[T]

  def kind: PatchMaker.Kind
}


sealed trait LowestPriorityPatchMaker {
  implicit def purePM[T]: PatchMaker[T] = PurePatchMaker[T]()
}

sealed trait StandardPatchMakers extends LowestPriorityPatchMaker {
  import PatchMaker._

  implicit def unorderedPM[F[_], T](implicit coll: UnorderedCollectionAdapter[F, T]): PatchMaker[F[T]] =
    UnorderedPatchMaker[F, T]()
}

sealed trait LowPriorityPatchMaker extends StandardPatchMakers {
  implicit def exportedPatchMaker[A](implicit exported: Exported[PatchMaker[A]]): PatchMaker[A] = exported.instance
}

object PatchMaker extends LowPriorityPatchMaker {
  import Patch._

  sealed trait Kind
  final object Kind {
    final case object Constant             extends Kind
    final case object Mono                 extends Kind
    final case object OrderedCollection    extends Kind
    final case object UnorderedCollection  extends Kind
    final case object IndexedCollection    extends Kind
    final case object KeyedCollection      extends Kind
    final case class Structure(id: String) extends Kind
    final case object Wrapper              extends Kind
    final case object Text                 extends Kind
  }

  def apply[T: PatchMaker]: PatchMaker[T] = implicitly

  class AbstractPatchMaker[T](k: Kind, mk: (T, T) => Patch[T]) extends PatchMaker[T] {
    override def kind: Kind                 = k
    override def make(l: T, r: T): Patch[T] = mk(l, r)
  }
  import Kind._

  private[gpl] case class PurePatchMaker[T]()
      extends AbstractPatchMaker[T](
        Constant,
        {
          case (null, r)        => SetValue(r)
          case (l, null)        => UnsetValue(l)
          case (l, r) if l == r => Empty
          case (l, r)           => UpdateValue(l, r)
        }
      )

  private[gpl] case class ArithmeticPatchMaker[T, D]()(implicit num: ArithmeticAdapter.Aux[T, D])
      extends AbstractPatchMaker[T](
        Mono,
        {
          case (l, r) if l == r                     => Empty
          case (l, r) if (l.getClass == r.getClass) =>
            import num._
            IncreaseValue(r - l)

          case (i, f) =>
            UpdateValue(i, f)
        }
      )

  implicit def arithmeticPM[T, D](implicit num: ArithmeticAdapter.Aux[T, D]): PatchMaker[T] =
    ArithmeticPatchMaker[T, D]()

  private[gpl] case class Sum1PatchMaker[F[_], T]()(implicit sum1: Sum1Adapter[F, T], pm: PatchMaker[T])
      extends AbstractPatchMaker[F[T]](
        Wrapper,
        {
          case (l, r) if l == r => Empty
          case (l, r)           =>
            sum1.extract(l, r) match {
              case None         => UpdateValue(l, r)
              case Some((l, r)) => Patch.make(l, r).imap(sum1.wrap, sum1.unwrap)
            }
        }
      ) {

    override def toString: String = s"SumPatchMaker(underlying=$pm)"
  }

  implicit def sum1PM[F[_], T: PatchMaker](implicit sum1: Sum1Adapter[F, T]): PatchMaker[F[T]] = Sum1PatchMaker[F, T]()

  private[gpl] case class Sum2PatchMaker[F[_, _], L, R]()(implicit
    sum2: Sum2Adapter[F, L, R],
    pmL: PatchMaker[L],
    pmR: PatchMaker[R]
  ) extends AbstractPatchMaker[F[L, R]](
        Wrapper,
        {
          case (l, r) if l == r => Empty
          case (l, r)           =>
            sum2.exract(l, r) match {
              case None                  => UpdateValue(l, r)
              case Some(Left((l0, l1)))  => Patch.make(l0, l1).imap(sum2.wrapLeft, sum2.unwrapLeft)
              case Some(Right((r0, r1))) => Patch.make(r0, r1).imap(sum2.wrapRight, sum2.unwrapRight)
            }
        }
      ) {

    override def toString: String = s"SumPatchMaker(underlying0=$pmL, underlying1=$pmR)"
  }

  implicit def sum2PM[F[_, _], L: PatchMaker, R: PatchMaker](implicit sum2: Sum2Adapter[F, L, R]): PatchMaker[F[L, R]] =
    Sum2PatchMaker[F, L, R]()

  private[gpl] case class UnorderedPatchMaker[F[_], T]()(implicit coll: UnorderedCollectionAdapter[F, T])
      extends AbstractPatchMaker[F[T]](
        UnorderedCollection,
        { case (l, r) =>
          import coll._

          val theDiff = diff(l, r)
          if (theDiff.events.isEmpty) Empty else UpdateUnordered(theDiff)
        }
      )

  private[gpl] case class OrderedPatchMaker[F[_], T]()(implicit coll: OrderedCollectionAdapter[F, T])
      extends AbstractPatchMaker[F[T]](
        OrderedCollection,
        { case (l, r) =>
          import coll._

          val theDiff = diff(l, r)
          if (theDiff.events.isEmpty) Empty else UpdateOrdered(theDiff)
        }
      )

  implicit def orderedPM[F[_], V](implicit coll: OrderedCollectionAdapter[F, V]): PatchMaker[F[V]] =
    OrderedPatchMaker[F, V]()

  private[gpl] case class IndexedPatchMaker[F[_], T]()(implicit coll: IndexedCollectionAdapter[F, T])
      extends AbstractPatchMaker[F[T]](
        IndexedCollection,
        { case (i, f) =>
          import coll._

          val (sizeDelta, delta) = diff(i, f)

          if (delta.isEmpty) {
            Patch.Empty
          } else {
            UpdateIndexed(delta, sizeDelta)
          }
        }
      ) {

    override def toString: String = s"IndexedPatchMaker(element=${coll.elementPatchMaker})"
  }

  implicit def indexedPM[F[_], V](implicit coll: IndexedCollectionAdapter[F, V]): PatchMaker[F[V]] =
    IndexedPatchMaker[F, V]()

  private[gpl] case class KeyedPatchMaker[F[_, _], K, V]()(implicit coll: KeyedCollectionAdapter[F, K, V])
      extends AbstractPatchMaker[F[K, V]](
        KeyedCollection,
        { case (i, f) =>
          import coll._

          val delta = diff(i, f)

          if (delta.isEmpty) {
            Patch.Empty
          } else {
            UpdateKeyed(delta)
          }
        }
      ) {

    override def toString: String = s"KeyedPatchMaker(value=${coll.valuePatchMaker})"
  }

  implicit def keyedPM[F[_, _], K, V](implicit coll: KeyedCollectionAdapter[F, K, V]): PatchMaker[F[K, V]] =
    KeyedPatchMaker[F, K, V]()
}
