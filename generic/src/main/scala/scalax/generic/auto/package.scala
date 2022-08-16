package scalax.generic

import scalax.patch.PatchMaker

package object auto {

  implicit def patchMaker[T: AutoDerivedPatchMaker]: PatchMaker[T] = implicitly[AutoDerivedPatchMaker[T]].patchMaker
}
