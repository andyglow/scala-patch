package scalax.patch

import org.bitbucket.cowwoc.diffmatchpatch._

package object texts {

  private[texts] val dmp = new DiffMatchPatch

  implicit val stringTextsPM: PatchMaker[String] =
    PatchMaker.mk(PatchMaker.Kind.Text) { case (l, r) =>
      TextPatch.make(l, r)
    }
}
