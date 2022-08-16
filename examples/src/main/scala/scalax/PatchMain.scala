package scalax

import java.time._

import scalax.patch._
import scalax.generic.auto._
import runtime.ScalaRunTime.stringOf

trait PatchMain {

  def doPatch[T: PatchMaker](l: T, r: T): Unit
}

object PatchMainRunner extends PatchMain with PatchMainPlus  {

  def doPatch[T](l: T, r: T)(implicit pm: PatchMaker[T]): Unit = {

    val patch = Patch.make(l, r)
    println(s"- { ----\n  pm: $pm\n  (left : ${stringOf(l)}) diff\n  (right: ${stringOf(r)}) =>\n  (patch: {\n${PatchVisitor stringify patch}  })")

    val patchedL = patch(l)
    val unpatchedR = patch.inverted(r)

    println()
    println(patch.toString)
    println()

    if (r != patchedL) println(s"  ERR: $r != ${stringOf(patchedL)}")
    if (l != unpatchedR) println(s"  ERR: $l != ${stringOf(unpatchedR)}")

    if (r == patchedL && l == unpatchedR) println("  OK")

    println("- } ----")
  }

  def main(args: Array[String]): Unit = {

    doPatch(
      3,
      3
    )

    doPatch(
      9,
      12
    )

    doPatch(
      Set(1, 2, 3),
      Set(2, 3, 4)
    )

    doPatch(
      Array(1, 2, 3),
      Array(2, 3, 4)
    )

    doPatch(
      Array(1, 2),
      Array(2, 3, 4)
    )

    doPatch(
      List(1, 2),
      List(2, 3, 4)
    )

    doPatch(
      Vector(1, 2),
      Vector(2, 3, 4)
    )

    doPatch(
      Map(1 -> 2, 2 -> 0),
      Map(2 -> 3)
    )

    doPatch(
      Map("a" -> Map("aa" -> "bb"), "b" -> Map("bb" -> "cc")),
      Map("c" -> Map("cc" -> "dd"), "a" -> Map("aa" -> "bbb", "aaa" -> "AAA"))
    )

    doPatch(
      Vector(1, 2),
      Vector(12)
    )

    doPatch(
      LocalDateTime.of(2020, 3, 3, 11, 0, 0),
      LocalDateTime.of(2020, 3, 4, 11, 0, 0)
    )

    doPatch(
      Instant.ofEpochMilli(1000),
      Instant.ofEpochMilli(1500)
    )

    doPatch(
      LocalDateTime.of(2020, 3, 3, 11, 0, 0),
      Instant.ofEpochMilli(1500)
    )

    doPatch(
      CC("shelly", 23, Map("prop1" -> "v1", "prop2" -> "v2"), Array(), true),
      CC("cristine", 37, Map("prop1" -> "vv1", "prop2" -> "vv2"), Array(), true)
    )

    doPatch[Option[Int]](
      Some(77),
      Some(127)
    )

    doPatch(
      None,
      Some(127)
    )

    doPatch[Either[String, Int]](
      Left("err1"),
      Left("err2")
    )

    doPatch[Either[String, Int]](
      Right(345),
      Right(2345)
    )

    doPatch(
      Left("abc"),
      Right(234)
    )


    doPatch(
      Array(CC("name", 23, Map("prop1" -> "v1", "prop2" -> "v2"), Array("foo"), true)),
      Array(CC("name", 24, Map("prop1" -> "v1", "prop2" -> "vv2"), Array("bar"), true))
    )

  }


  case class CC(
    name: String,
    age: Int,
    props: Map[String, String],
    logs: Array[String],
    active: Boolean
  )

  object CC {

//    implicit val ccpm: PatchMaker[CC] = {
//
//      case class $CC$Patch(
//        name: Patch[String],
//        age: Patch[Int],
//        props: Patch[Map[String, String]]) extends Patch[CC] {
//
//        def apply(x: CC): CC    = CC(name = name(x.name), age = age(x.age), props = props(x.props))
//        def isEmpty: Boolean    = name.isEmpty && age.isEmpty
//        def inverted: Patch[CC] = $CC$Patch(name.inverted, age.inverted, props.inverted)
//      }
//
//      PatchMaker.mk[CC] { case (l, r) =>
//        $CC$Patch(
//          name = Patch.make(l.name, r.name),
//          age = Patch.make(l.age, r.age),
//          props = Patch.make(l.props, r.props),
//        )
//      }
//    }


//    implicit val ccpm: PatchMaker[CC] = {
//      case class $CC$Patch(name: scalax.patch.Patch[String], age: scalax.patch.Patch[Int], props: scalax.patch.Patch[scala.collection.immutable.Map[String,String]]) extends scalax.patch.Patch[scalax.Main.CC] {
//        def apply(x: scalax.Main.CC): scalax.Main.CC = CC(name = this.name(x.name), age = this.age(x.age), props = this.props(x.props));
//        def isEmpty: Boolean = name.isEmpty.&&(age.isEmpty).&&(props.isEmpty);
//        def inverted: scalax.patch.Patch[scalax.Main.CC] = $CC$Patch(name = name.inverted, age = age.inverted, props = props.inverted)
//      };
//      PatchMaker.mk[scalax.Main.CC]({
//        case scala.Tuple2((l @ _), (r @ _)) => $CC$Patch(name = Patch.make(l.name, r.name), age = Patch.make(l.age, r.age), props = Patch.make(l.props, r.props))
//      })
//    }

//    implicit val ccpm: PatchMaker[CC] = PatchMaker.mk[scalax.Main.CC]({
//      case class $CC$Patch(name: scalax.patch.Patch[String], age: scalax.patch.Patch[Int], props: scalax.patch.Patch[scala.collection.immutable.Map[String,String]]) extends scalax.patch.Patch[scalax.Main.CC] {
//        def apply(x: scalax.Main.CC): scalax.Main.CC = CC(name = this.name(x.name), age = this.age(x.age), props = this.props(x.props));
//        def isEmpty: Boolean = name.isEmpty.&&(age.isEmpty).&&(props.isEmpty);
//        def inverted: scalax.patch.Patch[scalax.Main.CC] = $CC$Patch(name = name.inverted, age = age.inverted, props = props.inverted)
//      };
//      {
//        case scala.Tuple2((l @ _), (r @ _)) => $CC$Patch(name = Patch.make(l.name, r.name), age = Patch.make(l.age, r.age), props = Patch.make(l.props, r.props))
//      }
//    })


//    import scalax.generic.semiauto._
//    implicit val ccpm: PatchMaker[CC] = derivePatchMaker[CC]
  }
}
