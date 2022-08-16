package scalax.generic.auto

import scalax.patch.PatchMaker
import scalax.patch.macros.{PatchMakerMacro, UCommons, UPatchMakerDerivation}

import scala.language.experimental.macros
import scala.reflect.macros.blackbox

case class AutoDerivedPatchMaker[T](patchMaker: PatchMaker[T]) extends AnyVal

object AutoDerivedPatchMaker {

  implicit def derivedPatchMaker[T]: AutoDerivedPatchMaker[T] = macro AutoDerivedPatchMakerMacro.deriveImpl[T]
}

class AutoDerivedPatchMakerMacro(val c: blackbox.Context) extends UPatchMakerDerivation {
  import c.universe._

  def deriveImpl[T](implicit t: c.WeakTypeTag[T]): c.Expr[AutoDerivedPatchMaker[T]] = {
    val tree = derivePatchMaker(t.tpe)
    c.Expr[AutoDerivedPatchMaker[T]](q"scalax.generic.auto.AutoDerivedPatchMaker[${t.tpe}]($tree)")
  }
}
