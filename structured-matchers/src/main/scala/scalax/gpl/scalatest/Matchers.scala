package scalax.gpl.scalatest

import org.scalactic.Equality
import org.scalatest.matchers.{MatchResult, Matcher}
import scalax.patch.PatchMaker

trait Matchers {

  class StructuredMatcher[T](right: T)(implicit pm: PatchMaker[T], eq: Equality[T]) extends Matcher[T] {

    override def apply(left: T): MatchResult = {
      val equal = eq.areEqual(left, right)
      val message = if (equal) "" else {
        val report = MismatchReport.compute(left, right)
        report.mismatches.map { case (branch, msg) => s"- $branch: $msg" }.mkString("\n")
      }
      MatchResult(
        equal,
        s"mismatches found:\n${message}",
        "no mismatches found"
      )
    }
  }

  def beTheSameAs[T: PatchMaker: Equality](right: T) = new StructuredMatcher[T](right)
}

object Matchers extends Matchers