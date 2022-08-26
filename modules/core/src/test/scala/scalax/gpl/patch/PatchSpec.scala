package scalax.gpl.patch

import java.time._

import org.scalactic.source.Position
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers._
//import scalax.gpl.patch.PatchMaker.purely._
import scalax.gpl.patch.PatchMatchers._

import scala.runtime.ScalaRunTime.stringOf

trait PatchSpecs { this: AnyFunSuite =>

  def doPatch[T: PatchMaker: MutationFighter](l: T, r: T)(implicit pos: Position): Unit = {
    val pm = PatchMaker[T]
    test(s"${stringOf(l)} | ${stringOf(r)} ($pm)") {
      val patch  = pm.make(l, r)
      val iPatch = patch.inverted

      patch should applyTo(MutationFighter copy l, MutationFighter copy r)
      iPatch should applyTo(MutationFighter copy r, MutationFighter copy l)
    }
  }
}

class PatchSpec extends AnyFunSuite with PatchSpecs with ScalaVersionSpecificPatchSpecs {

  doPatch(
    9,
    12
  )

  doPatch(
    Seq(1, 2, 3),
    Seq(2, 3, 4)
  )

  doPatch(
    Array(1, 2, 3),
    Array(2, 3, 4)
  )

  doPatch(
    Array(1, 2),
    Array(2, 3, 4)
  )

  doPatch[List[Int]](
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

  // java.util, java.sql
  doPatch(
    java.util.Date.from(Instant.ofEpochSecond(28374567)),
    java.util.Date.from(Instant.ofEpochSecond(28374568))
  )

  doPatch(
    java.sql.Date.valueOf(LocalDate.of(2020, 4, 6)),
    java.sql.Date.valueOf(LocalDate.of(2020, 5, 12))
  )

  doPatch(
    java.sql.Time.valueOf(LocalTime.of(18, 6)),
    java.sql.Time.valueOf(LocalTime.of(12, 12))
  )

  doPatch(
    java.sql.Timestamp.from(Instant.ofEpochSecond(28374567)),
    java.sql.Timestamp.from(Instant.ofEpochSecond(28374568))
  )

}
