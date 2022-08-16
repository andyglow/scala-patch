package scalax.generic

import scalax.patch.PatchMaker
import scalax.patch.macros.PatchMakerMacro

import scala.language.experimental.macros


object semiauto {

  def derivePatchMaker[T]: PatchMaker[T] = macro PatchMakerMacro.deriveImpl[T]
}
