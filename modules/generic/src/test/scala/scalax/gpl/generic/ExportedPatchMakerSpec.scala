package scalax.gpl.generic

import org.scalatest.matchers.should.Matchers._
import org.scalatest.funsuite.AnyFunSuite
import scalax.gpl.generic.auto._
import scalax.gpl.patch.Patch.IncreaseValue
import scalax.gpl.patch.{Patch, PatchMaker}

class ExportedPatchMakerSpec extends AnyFunSuite {
  import ExportedPatchMakerSpec._

  test("automatically derive patch") {
    val pm = implicitly[PatchMaker[CC]]
    pm.toString shouldBe "$CC$PatchMaker(id=PurePatchMaker(), name=SumPatchMaker(underlying=PurePatchMaker()), age=ArithmeticPatchMaker())"
    pm.getClass.getSimpleName should fullyMatch regex """\$CC\$PatchMaker\$\d+"""

    val cc1 = CC("abc", Some("ABC"), 5)
    val cc2 = CC("abc", Some("ABC"), 6)

    val patch = Patch.make(cc1, cc2)
    patch.isOpaque shouldBe false
    patch.getClass.getSimpleName should fullyMatch regex """\$CC\$Patch\$\d+"""
    patch shouldBe a[Product]
    val pp = patch.asInstanceOf[Product]
    pp.productArity shouldBe 3
    pp.productElement(0) shouldBe Patch.Empty[String]
    pp.productElement(1) shouldBe Patch.Empty[Option[String]]
    pp.productElement(2) shouldBe IncreaseValue[Int, Int](1)
  }
}

object ExportedPatchMakerSpec {

  case class CC(id: String, name: Option[String], age: Int)
}
