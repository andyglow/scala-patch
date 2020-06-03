package scalax.patch.macros

import scalax.patch._


private[macros] trait Logic extends Extractors with HasLog with HasContext {
  import c.universe._

  val patchMaker = typeOf[PatchMaker[_]]
  val patch      = typeOf[Patch[_]]
}
