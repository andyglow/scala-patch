package scalax.gpl.generic

import scalax.gpl.patch.PatchMaker
import scalax.gpl.patch.macros.PatchMakerMacro

import scala.language.experimental.macros

object semiauto {

  def derivePatchMaker[T]: PatchMaker[T] = macro PatchMakerMacro.deriveImpl[T]
}
