package scalax.gpl.generic.auto

import scalax.gpl.Exported
import scalax.gpl.patch.PatchMaker
import scalax.gpl.patch.macros.UPatchMakerDerivation

import scala.language.experimental.macros
import scala.reflect.macros.blackbox

class ExportedPatchMakerMacro(val c: blackbox.Context) extends UPatchMakerDerivation {
  import c.universe._

  def deriveImpl[T](implicit t: c.WeakTypeTag[T]): c.Expr[Exported[PatchMaker[T]]] = {
    val tree = derivePatchMaker(t.tpe)
    val x    = reify(Exported(c.Expr[PatchMaker[T]](tree).splice))

    c.info(c.enclosingPosition, showCode(x.tree), force = true)

    x
  }
}
