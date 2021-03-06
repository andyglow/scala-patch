package scalax.patch

import scalax.patch.adapter._
import scalax.patch.adapter.collections._
import scalax.ScalaVersionSpecificUtils._

/** Represents a Patch function
  *
  * @tparam T type that can by patched with this Patch
  */
trait Patch[T] { self =>
  import Patch._

  /** Does this Patch really going to change the value
    *
    * @return
    */
  def isOpaque: Boolean

  /** Does this Patch really going to change the value
    *
    * @return
    */
  def nonOpaque: Boolean = !isOpaque

  /** Do patch the given value
    *
    * @param x a value to patch
    * @return patched value
    */
  def apply(x: T): T

  /** Creates a Patch tha can be used to patch back (un-patch)
    *
    * @return Inverted Patch
    */
  def inverted: Patch[T]

  /** Makes a wrapping patch.
    * Used for Sum types: Option, Either
    *
    * @param fw Function that converts a value to Monadic type (wrap)
    * @param bk Function that converts Monadic value back to original value (un-wrap)
    * @tparam TT
    * @return Wrapped Patch
    */
  def imap[TT](fw: T => TT, bk: TT => T): Patch[TT] = MappedPatch[T, TT](this)(fw, bk)

  /** Reports some internal representation details so developer can have access to Patch function specific details.
    * Can be used for rendering to text.
    *
    * @param x Renderer
    */
  def render(x: PatchVisitor): Unit
}

object Patch {

  def make[T](l: T, r: T)(implicit p: PatchMaker[T]): Patch[T] = p.make(l, r)

  case class MappedPatch[T, TT](underlying: Patch[T])(fw: T => TT, bk: TT => T) extends Patch[TT] {

    override def isOpaque: Boolean = underlying.isOpaque

    override def apply(x: TT): TT = fw(underlying(bk(x)))

    override def inverted: Patch[TT] = underlying.inverted.imap(fw, bk)

    def render(x: PatchVisitor): Unit = underlying.render(x)
  }

  private val _empty: Patch[Any] = new Patch[Any] {

    def isOpaque = true

    def apply(x: Any): Any = x

    override def inverted: Patch[Any] = this

    def render(x: PatchVisitor): Unit = ()
  }

  def Empty[T]: Patch[T] = _empty.asInstanceOf[Patch[T]]

  case class Group[T](steps: List[Patch[T]]) extends Patch[T] {

    def isOpaque = steps.isEmpty

    def apply(x: T): T = steps.foldLeft(x) { case (x, s) => s(x) }

    def inverted: Patch[T] = Group(steps map { _.inverted })

    def render(x: PatchVisitor): Unit = steps foreach { _.render(x) }
  }
  object Group {

    def apply[T](x: Patch[T], xs: Patch[T]*): Group[T] = Group(x +: xs.toList)
  }

  case class UpdateValue[T](from: T, to: T) extends Patch[T] {

    def isOpaque = false

    def apply(x: T): T = to

    def inverted = UpdateValue(to, from)

    def render(x: PatchVisitor): Unit = x.updateValue(from, to)
  }

  case class SetValue[T](to: T) extends Patch[T] {

    def isOpaque = false

    def apply(x: T): T = to

    def inverted = UnsetValue(to)

    def render(x: PatchVisitor): Unit = x.setValue(to)
  }

  case class UnsetValue[T](from: T) extends Patch[T] {

    def isOpaque = false

    def apply(x: T): T = null.asInstanceOf[T]

    def inverted = SetValue(from)

    def render(x: PatchVisitor): Unit = x.unsetValue(from)
  }

  case class IncreaseValue[T, D](delta: D)(implicit lin: ArithmeticAdapter.Aux[T, D]) extends Patch[T] {
    import lin._

    def isOpaque = false

    def apply(x: T): T = x :+ delta

    def inverted = DecreaseValue(delta)

    def render(x: PatchVisitor): Unit = x.increaseValue(delta)
  }

  case class DecreaseValue[T, D](delta: D)(implicit lin: ArithmeticAdapter.Aux[T, D]) extends Patch[T] {
    import lin._

    def isOpaque = false

    def apply(x: T): T = x :- delta

    def inverted = IncreaseValue(delta)

    def render(x: PatchVisitor): Unit = x.decreaseValue(delta)
  }

  case class UpdateUnordered[F[_], T](delta: UnorderedCollectionAdapter.Diff[T])(implicit adapt: UnorderedCollectionAdapter[F, T]) extends Patch[F[T]] {
    import UnorderedCollectionAdapter._
    import Diff._
    import Evt._
    import adapt._

    def isOpaque = delta.events.isEmpty

    def apply(x: F[T]): F[T] = x.applyDiff(delta)

    def inverted = UpdateUnordered(delta.inverted)

    def render(x: PatchVisitor): Unit = delta.events foreach {
      case Add(es)    => x.addItems(es)
      case Remove(es) => x.removeItems(es)
    }
  }

  case class UpdateOrdered[F[_], T](delta: OrderedCollectionAdapter.Diff[T])(implicit adapt: OrderedCollectionAdapter[F, T]) extends Patch[F[T]] {
    import OrderedCollectionAdapter._
    import Diff._
    import Evt._
    import adapt._

    def isOpaque = delta.events.isEmpty

    def apply(x: F[T]): F[T] = x.applyDiff(delta)

    def inverted = UpdateOrdered(delta.inverted)

    def render(x: PatchVisitor): Unit = delta.events foreach {
      case Skip(n)      => x.skipItems(n)
      case Insert(es)   => x.insertItems(es)
      case Drop(es)     => x.dropItems(es)
      case Upgrade(es)  => x.upgradeItems(es)
    }
  }

  case class UpdateIndexed[F[_], V](delta: Map[Int, Patch[V]], sizeDelta: Int)(implicit adapt: IndexedCollectionAdapter[F, V]) extends Patch[F[V]] {
    import adapt._

    def isOpaque = delta.isEmpty

    def apply(x: F[V]): F[V] = {
      if (sizeDelta > 0) {
        x resized sizeDelta updatedWith delta
      } else if (sizeDelta == 0) {
        x updatedWith delta
      } else {
        val newSize = x.size + sizeDelta
        val reducedDelta = delta collect { case (i, p) if i < newSize => (i, p) }
        { x resized sizeDelta } updatedWith reducedDelta
      }
    }

    def inverted = UpdateIndexed[F, V](mapValues[Int, Patch[V], Patch[V]](delta, _.inverted), - sizeDelta)

    def render(x: PatchVisitor): Unit = {
      if (sizeDelta != 0) x.resize(sizeDelta)
      delta.keys.toList.sorted foreach { i =>
        x.intoIndex(i)
        delta(i).render(x)
        x.outofIndex(i)
      }
    }
  }

  case class UpdateKeyed[F[_, _], K, V](delta: Map[K, Patch[V]])(implicit adapt: KeyedCollectionAdapter[F, K, V]) extends Patch[F[K, V]] {
    import adapt._

    def isOpaque: Boolean = delta.isEmpty

    def apply(x: F[K, V]): F[K, V] = x updatedWith delta

    def inverted: Patch[F[K, V]] = UpdateKeyed[F, K, V](mapValues[K, Patch[V], Patch[V]](delta, _.inverted))

    def render(x: PatchVisitor): Unit = delta foreach { case (k, v) =>
      x.intoKey(k)
      v.render(x)
      x.outofKey(k)
    }
  }

}
