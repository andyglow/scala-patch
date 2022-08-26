package scalax.gpl.scalatest

import org.scalactic.Equality
import org.scalatest.matchers.{MatchResult, Matcher}
import scalax.gpl.patch.{Patch, PatchMaker}

trait Matchers {

  class StructuredMatcher[T](right: T)(implicit pm: PatchMaker[T]) extends Matcher[T] {

    override def apply(left: T): MatchResult = {
      val patch = Patch.make(left, right)

      val message =
        if (patch.isOpaque) ""
        else {
          val report = MismatchReport.compute(patch)
          report.mismatches.map { case (branch, msg) => s"- $branch: $msg" }.mkString("\n")
        }
      MatchResult(
        patch.isOpaque,
        s"mismatches found:\n${message}",
        "no mismatches found"
      )
    }
  }

  def beTheSameAs[T: PatchMaker: Equality](right: T) = new StructuredMatcher[T](right)
}

object Matchers extends Matchers
