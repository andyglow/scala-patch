package scalax.gpl.generic

import org.scalatest.matchers.should.Matchers._
import org.scalatest.funsuite.AnyFunSuite
import scalax.gpl.patch.Patch.{UpdateIndexed, UpdateValue}
import scalax.gpl.patch.ProductPatch
import scalax.gpl.generic.auto._
import scalax.gpl.patch.Patch.IncreaseValue
import scalax.gpl.patch.{Patch, PatchMaker}

class ExportedPatchMakerSpec extends AnyFunSuite {
  import ExportedPatchMakerSpec._

  test("automatically derive patch") {
    val pm = implicitly[PatchMaker[CC]]
    pm.toString shouldBe "ProductPatch.Maker[CC](id=PurePatchMaker(), name=SumPatchMaker(underlying=PurePatchMaker()), age=ArithmeticPatchMaker(), meta=IndexedPatchMaker(element=ProductPatch.Maker[KV](k=PurePatchMaker(), v=PurePatchMaker())))"

    val cc1 = CC("abc", Some("ABC"), 5, Array(KV("a", "b"), KV("c", "d")))
    val cc2 = CC("abc", Some("ABC"), 6, Array(KV("a", "c"), KV("e", "f")))

    val patch = Patch.make(cc1, cc2)
    patch.isOpaque shouldBe false
    patch shouldBe ProductPatch.Patch4[CC, String, Option[String], Int, Array[KV]](
      "CC",
      "id",
      _.id,
      Patch.Empty[String],
      "name",
      _.name,
      Patch.Empty[Option[String]],
      "age",
      _.age,
      IncreaseValue[Int, Int](1),
      "meta",
      _.meta,
      UpdateIndexed(
        Map(
          0 -> ProductPatch
            .Patch2[KV, String, String]("KV", "k", _.k, Patch.Empty[String], "v", _.v, UpdateValue("b", "c"), KV.apply),
          1 -> ProductPatch.Patch2[KV, String, String](
            "KV",
            "k",
            _.k,
            UpdateValue("c", "e"),
            "v",
            _.v,
            UpdateValue("d", "f"),
            KV.apply
          )
        ),
        0
      ),
      CC.apply
    )
  }
}

object ExportedPatchMakerSpec {

  case class CC(id: String, name: Option[String], age: Int, meta: Array[KV])
  case class KV(k: String, v: String)
}
