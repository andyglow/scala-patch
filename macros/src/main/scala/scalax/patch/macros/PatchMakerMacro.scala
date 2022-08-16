package scalax.patch.macros

import scalax.patch.PatchMaker

import scala.reflect.macros.blackbox

class PatchMakerMacro(val c: blackbox.Context) extends UPatchMakerDerivation {
  import c.universe._

  def deriveImpl[T](implicit t: c.WeakTypeTag[T]): c.Expr[PatchMaker[T]] = {
    val tree = derivePatchMaker(t.tpe)

    if (c.settings.contains("print-patch-maker-code"))
      info(showCode(tree))

    c.Expr[PatchMaker[T]](tree)
  }
}
