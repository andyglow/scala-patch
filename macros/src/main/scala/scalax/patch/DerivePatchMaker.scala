package scalax.patch

import scalax.patch.macros.PatchMakerMacro


object DerivePatchMaker {

  def derive[T]: PatchMaker[T] = macro PatchMakerMacro.deriveImpl[T]
}
