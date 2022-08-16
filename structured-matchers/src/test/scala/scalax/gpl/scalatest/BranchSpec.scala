package scalax.gpl.scalatest

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers._
import scalax.gpl.scalatest.MismatchReport._

class BranchSpec extends AnyFunSuite {

  test("append") {
    val b = new MutableBranch()
    b.append("foo")
    b.append(5)
    b.append(new StringBuilder("zzz"))

    b.elements should contain inOrderOnly(
      BranchElement.StructField("foo"),
      BranchElement.CollectionIndex(5),
      BranchElement.MapKey(new StringBuilder("zzz"))
    )
  }

  test("shrink") {
    val b = new MutableBranch()
    b.append("foo")
    b.append(5)
    b.append(new StringBuilder("zzz"))
    b.shrink()
    b.shrink()

    b.elements should contain only(
      BranchElement.StructField("foo")
    )
  }

  test("toString") {
    val b = new MutableBranch()
    b.append(Root)
    b.append("foo")
    b.append(5)
    b.append(new StringBuilder("zzz"))

    b.copy().toString shouldBe "root.foo[5].zzz"
  }
}
