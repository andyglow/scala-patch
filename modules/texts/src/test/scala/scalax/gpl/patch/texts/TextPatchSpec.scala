package scalax.gpl.patch.texts

import org.scalatest.matchers.should.Matchers._
import org.scalatest.funsuite.AnyFunSuite

class TextPatchSpec extends AnyFunSuite {
  import TextPatch._
  import Evt._

  test("make") {
    TextPatch.make("abc", "abc") shouldBe TextPatch.Empty
    TextPatch.make("abc", "def") shouldBe TextPatch(Step(0, 0, 3, 3, Delete("abc"), Insert("def")))
    TextPatch.make("abc", "abcdef") shouldBe TextPatch(Step(0, 0, 3, 6, Equal("abc"), Insert("def")))
    TextPatch.make("abc", "defabc") shouldBe TextPatch(Step(0, 0, 3, 6, Insert("def"), Equal("abc")))
    TextPatch.make("abc", "") shouldBe TextPatch(Step(0, 0, 3, 0, Delete("abc")))

    TextPatch.make("hello, dear friend!", "hello, my friend!") shouldBe TextPatch(
      Step(3, 3, 12, 10, Equal("lo, "), Delete("dear"), Insert("my"), Equal(" fri"))
    )
    TextPatch.make("hello, dear friend!", "good bye, my friend!") shouldBe TextPatch(
      Step(0, 0, 15, 16, Delete("hello, dear"), Insert("good bye, my"), Equal(" fri"))
    )
  }

  test("apply") {
    TextPatch.Empty("abc") shouldBe "abc"
    TextPatch(Step(0, 0, 3, 3, Delete("abc"), Insert("def"))) apply "abc" shouldBe "def"
    TextPatch(Step(0, 0, 3, 6, Equal("abc"), Insert("def"))) apply "abc" shouldBe "abcdef"
    TextPatch(Step(0, 0, 3, 6, Insert("def"), Equal("abc"))) apply "abc" shouldBe "defabc"
    TextPatch(Step(0, 0, 3, 0, Delete("abc"))) apply "abc" shouldBe ""

    TextPatch(
      Step(3, 3, 12, 10, Equal("lo, "), Delete("dear"), Insert("my"), Equal(" fri"))
    ) apply "hello, dear friend!" shouldBe "hello, my friend!"
    TextPatch(
      Step(0, 0, 15, 16, Delete("hello, dear"), Insert("good bye, my"), Equal(" fri"))
    ) apply "hello, dear friend!" shouldBe "good bye, my friend!"
  }
}
