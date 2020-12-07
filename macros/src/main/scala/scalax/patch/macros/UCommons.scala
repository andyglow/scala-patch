package scalax.patch.macros

import scalax.Default
import scalax.patch._


private[macros] trait UCommons extends UProductTypes with ULogging with UContext with UParameters with UMaker {
  import c.universe._

  val patchMaker   = typeOf[PatchMaker[_]]
  val patch        = typeOf[Patch[_]]
  val patchVisitor = typeOf[PatchVisitor]
  val default      = typeOf[Default[_]]

  def suffixName(x: Tree): Name = x match {
    case Select(_, x) => x
    case _            => err(s"Unexpected tree ${showRaw(x)}. Expected Select(...)")
  }
}
