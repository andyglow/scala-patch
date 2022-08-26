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
      "id", _.id, Patch.Empty[String],
      "name", _.name, Patch.Empty[Option[String]],
      "age", _.age, IncreaseValue[Int, Int](1),
      "meta", _.meta, UpdateIndexed(Map(
        0 -> ProductPatch.Patch2[KV, String, String]("KV", "k", _.k, Patch.Empty[String], "v", _.v, UpdateValue("b", "c"), KV.apply),
        1 -> ProductPatch.Patch2[KV, String, String]("KV", "k", _.k, UpdateValue("c", "e"), "v", _.v, UpdateValue("d", "f"), KV.apply)
      ), 0),
      CC.apply
    )
  }
}

object ExportedPatchMakerSpec {

  case class CC(id: String, name: Option[String], age: Int, meta: Array[KV])
  case class KV(k: String, v: String)
//
//  implicit val ccPM: PatchMaker[CC] = Exported.apply({
//    case class $CC$Patch(
//      id: scalax.gpl.patch.Patch[String],
//      name: scalax.gpl.patch.Patch[Option[String]],
//      age: scalax.gpl.patch.Patch[Int],
//      meta: scalax.gpl.patch.Patch[Array[scalax.gpl.generic.ExportedPatchMakerSpec.KV]]) extends _root_.scalax.gpl.patch.Patch[scalax.gpl.generic.ExportedPatchMakerSpec.CC] {
//      def apply(x: scalax.gpl.generic.ExportedPatchMakerSpec.CC): scalax.gpl.generic.ExportedPatchMakerSpec.CC = CC(id = this.id(x.id), name = this
//        .name(x.name), age = this.age(x.age), meta = this.meta(x.meta));
//
//      def isOpaque: Boolean = id.isOpaque.&&(name.isOpaque).&&(age.isOpaque).&&(meta.isOpaque);
//
//      def inverted: scalax.gpl.patch.Patch[scalax.gpl.generic.ExportedPatchMakerSpec.CC] = $CC$Patch(id = id.inverted, name = name.inverted, age = age
//        .inverted, meta = meta.inverted);
//
//      def visit(x: scalax.gpl.patch.PatchVisitor): Unit = {
//        if (this.id.nonOpaque) {
//          x.intoField("id");
//          this.id.visit(x);
//          x.outofField("id")
//        }
//        else
//          ();
//        if (this.name.nonOpaque) {
//          x.intoField("name");
//          this.name.visit(x);
//          x.outofField("name")
//        }
//        else
//          ();
//        if (this.age.nonOpaque) {
//          x.intoField("age");
//          this.age.visit(x);
//          x.outofField("age")
//        }
//        else
//          ();
//        if (this.meta.nonOpaque) {
//          x.intoField("meta");
//          this.meta.visit(x);
//          x.outofField("meta")
//        }
//        else
//          ()
//      };
//
//      override def toString(): String = "$CC$Patch".+("(").+("id".+("=").+(id.toString).+(", ").+("name".+("=").+(name.toString)).+(", ")
//                                                                 .+("age".+("=").+(age.toString)).+(", ").+("meta".+("=").+(meta.toString))).+(")")
//    };
//    case class $CC$PatchMaker() extends _root_.scalax.gpl.patch.PatchMaker.AbstractPatchMaker[scalax.gpl.generic.ExportedPatchMakerSpec.CC](_root_
//      .scalax.gpl.patch.PatchMaker.Kind.Structure("CC"), {
//      case scala.Tuple2((l@_), (r@_)) => $CC$Patch(id = $CC$PatchMakerHolder.$id$pm.make(l.id, r.id), name = $CC$PatchMakerHolder.$name$pm
//                                                                                                                                 .make(l.name, r
//                                                                                                                                   .name), age = $CC$PatchMakerHolder
//        .$age$pm.make(l.age, r.age), meta = $CC$PatchMakerHolder.$meta$pm.make(l.meta, r.meta))
//    }) {
//      override def toString(): String = "$CC$PatchMaker".+("(").+("id".+("=").+($CC$PatchMakerHolder.$id$pm.toString).+(", ")
//                                                                      .+("name".+("=").+($CC$PatchMakerHolder.$name$pm.toString)).+(", ")
//                                                                      .+("age".+("=").+($CC$PatchMakerHolder.$age$pm.toString)).+(", ")
//                                                                      .+("meta".+("=").+($CC$PatchMakerHolder.$meta$pm.toString))).+(")")
//    };
//    object $CC$PatchMakerHolder {
//      lazy val $id$pm: PatchMaker[String] = scalax.gpl.patch.PatchMaker.purePM[String];
//      lazy val $name$pm: PatchMaker[Option[String]] = scalax.gpl.patch.PatchMaker
//                                                            .sum1PM[Option, String](scalax.gpl.patch.PatchMaker.purePM[String], scalax.gpl.patch
//                                                                                                                                      .adapter
//                                                                                                                                      .Sum1Adapter
//                                                                                                                                      .forOption[String]);
//      lazy val $age$pm: PatchMaker[Int] = scalax.gpl.patch.PatchMaker.arithmeticPM[Int, Int](scalax.gpl.patch.adapter.ArithmeticAdapter
//                                                                                                   .forNumeric[Int](scala.math.Numeric
//                                                                                                                         .IntIsIntegral));
//      lazy val $meta$pm: PatchMaker[Array[scalax.gpl.generic.ExportedPatchMakerSpec.KV]] = scalax.gpl.patch.PatchMaker
//                                                                                                 .indexedPM[Array, scalax.gpl.generic.ExportedPatchMakerSpec.KV](scalax
//                                                                                                   .gpl.patch.adapter.collections
//                                                                                                   .IndexedCollectionAdapter
//                                                                                                   .forArray[scalax.gpl.generic.ExportedPatchMakerSpec.KV](scalax
//                                                                                                     .gpl.patch.PatchMaker
//                                                                                                     .exportedPatchMaker[scalax.gpl.generic.ExportedPatchMakerSpec.KV](((Exported
//                                                                                                       .apply[scalax.gpl.patch.PatchMaker[scalax.gpl.generic.ExportedPatchMakerSpec.KV]]({
//                                                                                                         case class $KV$Patch(
//                                                                                                           k: scalax.gpl.patch.Patch[String],
//                                                                                                           v: scalax.gpl.patch.Patch[String]) extends _root_.scalax.gpl.patch.Patch[scalax.gpl.generic.ExportedPatchMakerSpec.KV] {
//                                                                                                           def apply(x: scalax.gpl.generic.ExportedPatchMakerSpec.KV): scalax.gpl.generic.ExportedPatchMakerSpec.KV = ExportedPatchMakerSpec
//                                                                                                             .KV.apply(this.k.apply(x.k), this.v
//                                                                                                                                              .apply(x
//                                                                                                                                                .v));
//
//                                                                                                           def isOpaque: scala.Boolean = $KV$Patch
//                                                                                                             .this.k.isOpaque
//                                                                                                             .&&($KV$Patch.this.v.isOpaque);
//
//                                                                                                           def inverted: scalax.gpl.patch.Patch[scalax.gpl.generic.ExportedPatchMakerSpec.KV] = $KV$Patch
//                                                                                                             .apply($KV$Patch.this.k
//                                                                                                                             .inverted, $KV$Patch.this
//                                                                                                                                                 .v
//                                                                                                                                                 .inverted);
//
//                                                                                                           def visit(x: scalax.gpl.patch.PatchVisitor): scala.Unit = {
//                                                                                                             if (this.k.nonOpaque) {
//                                                                                                               x.intoField("k");
//                                                                                                               this.k.visit(x);
//                                                                                                               x.outofField("k")
//                                                                                                             }
//                                                                                                             else
//                                                                                                               ();
//                                                                                                             if (this.v.nonOpaque) {
//                                                                                                               x.intoField("v");
//                                                                                                               this.v.visit(x);
//                                                                                                               x.outofField("v")
//                                                                                                             }
//                                                                                                             else
//                                                                                                               ()
//                                                                                                           };
//
//                                                                                                           override def toString(): scala.Predef.String = "$KV$Patch("
//                                                                                                             .+("k=".+($KV$Patch.this.k.toString())
//                                                                                                                    .+(", ").+("v="
//                                                                                                               .+($KV$Patch.this.v.toString())))
//                                                                                                             .+(")")
//                                                                                                         };
//                                                                                                         case class $KV$PatchMaker() extends _root_.scalax.gpl.patch.PatchMaker.AbstractPatchMaker[scalax.gpl.generic.ExportedPatchMakerSpec.KV](scalax
//                                                                                                           .gpl.patch.PatchMaker.Kind.Structure
//                                                                                                           .apply("KV"), ((x0$6: scalax.gpl.generic.ExportedPatchMakerSpec.KV, x1$6: scalax.gpl.generic.ExportedPatchMakerSpec.KV) => scala
//                                                                                                           .Tuple2
//                                                                                                           .apply[scalax.gpl.generic.ExportedPatchMakerSpec.KV, scalax.gpl.generic.ExportedPatchMakerSpec.KV](x0$6, x1$6) match {
//                                                                                                           case scala.Tuple2((l@_), (r@_)) => $KV$Patch
//                                                                                                             .apply($KV$PatchMakerHolder.$k$pm
//                                                                                                                                        .make(l.k, r
//                                                                                                                                          .k), $KV$PatchMakerHolder
//                                                                                                               .$v$pm.make(l.v, r.v))
//                                                                                                         })) {
//                                                                                                           override def toString(): scala.Predef.String = "$KV$PatchMaker("
//                                                                                                             .+("k=".+($KV$PatchMakerHolder.$k$pm
//                                                                                                                                           .toString())
//                                                                                                                    .+(", ").+("v="
//                                                                                                               .+($KV$PatchMakerHolder.$v$pm
//                                                                                                                                      .toString())))
//                                                                                                             .+(")")
//                                                                                                         };
//                                                                                                         object $KV$PatchMakerHolder {
//                                                                                                           lazy val $k$pm: scalax.gpl.patch.PatchMaker[String] = scalax
//                                                                                                             .gpl.patch.PatchMaker.purePM[String];
//                                                                                                           lazy val $v$pm: scalax.gpl.patch.PatchMaker[String] = scalax
//                                                                                                             .gpl.patch.PatchMaker.purePM[String]
//                                                                                                         };
//                                                                                                         new $KV$PatchMaker()
//                                                                                                       })): scalax.gpl.Exported[scalax.gpl.patch.PatchMaker[scalax.gpl.generic.ExportedPatchMakerSpec.KV]]))))
//    };
//    new $CC$PatchMaker()
//  }).instance
}
