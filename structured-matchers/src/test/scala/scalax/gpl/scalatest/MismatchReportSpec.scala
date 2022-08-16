package scalax.gpl.scalatest

import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpec
import scalax.patch._

class MismatchReportSpec extends AnyWordSpec {
  import MismatchReportSpec._

  "MismatchReport" should {

    "generate report for arrays" in {
      MismatchReport.compute(
        Array(20),
        Array(22, 5)
      ).toString shouldBe
        """- root[0]: expected value is 2 bigger then actual
          |- root[1]: expected: 5, actual: null""".stripMargin
    }

    "generate report for case classes" in {
      MismatchReport.compute(
        CC("foo", Some(Array(KV(1, "11"), KV(2, "22")))),
        CC("bar", Some(Array(KV(1, "11"), KV(2, "222")))),
      ).toString shouldBe
        """- root.id: expected: bar, actual: foo
          |- root.nested[1].value: expected: 222, actual: 22""".stripMargin
    }
  }
}

object MismatchReportSpec {
  import scalax.generic.semiauto._

  case class KV(key: Int, value: String)
  object KV {
    implicit def kvPatchMaker[K, V: PatchMaker]: PatchMaker[KV] = derivePatchMaker
  }

  case class CC(
    id: String,
    nested: Option[Array[KV]]
  )

  object CC {
    implicit val ccPatchMaker: PatchMaker[CC] = derivePatchMaker
  }
}