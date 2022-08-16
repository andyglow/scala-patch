package scalax.patch

import org.bitbucket.cowwoc.diffmatchpatch._

package object texts {

  private[texts] val dmp = new DiffMatchPatch

  object TextPatchMaker extends PatchMaker[String] {
    override def make(l: String, r: String): Patch[String] = TextPatch.make(l, r)

    override def kind: PatchMaker.Kind = PatchMaker.Kind.Text
  }


  implicit val stringTextsPM: PatchMaker[String] = TextPatchMaker
}
