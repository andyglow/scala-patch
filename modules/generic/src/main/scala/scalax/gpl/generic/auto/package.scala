package scalax.gpl.generic

import scalax.gpl.Exported
import scalax.gpl.patch.PatchMaker

import scala.language.experimental.macros

package object auto {

  implicit def exportedPatchMaker[T]: Exported[PatchMaker[T]] = macro ExportedPatchMakerMacro.deriveImpl[T]
}
