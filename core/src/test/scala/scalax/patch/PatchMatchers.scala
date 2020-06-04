package scalax.patch

import org.scalatest.matchers.{MatchResult, Matcher}

trait PatchMatchers {

  class ApplyMatcher[T](l: T, r: T)(implicit pm: PatchMaker[T]) extends Matcher[Patch[T]] {

    override def apply(patch: Patch[T]): MatchResult = {
      val originalL = if (l.getClass.isArray) {
        // a trick to save original mutable array for later error report
        val arr = l.asInstanceOf[Array[_]]
        arr.toList.toArray
      } else l

      val ll = patch(l)
      val eq: Boolean = if (l.getClass.isArray) {
        val left = ll.asInstanceOf[Array[_]].toList
        val right = r.asInstanceOf[Array[_]].toList
        left == right
      } else if (pm.kind == PatchMaker.Kind.UnorderedCollection) {
        (ll.asInstanceOf[Seq[T]] diff r.asInstanceOf[Seq[T]]).isEmpty
      } else
        ll == r

      MatchResult(
        eq,
        s"""Patch $patch application failed. {
           |${PatchVisitor stringify patch}
           |}
           |Original : ${runtime.ScalaRunTime.stringOf(originalL)}
           |Expected : ${runtime.ScalaRunTime.stringOf(r)}
           |Actual   : ${runtime.ScalaRunTime.stringOf(ll)}
           |""".stripMargin,
        s"""Patch $patch applied successfully""""
      )
    }
  }

  def applyTo[T: PatchMaker](l: T, r: T) = new ApplyMatcher[T](l, r)
}

object PatchMatchers extends PatchMatchers