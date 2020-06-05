package scalax.patch.texts

import java.{ util => ju }

import scalax.patch._
import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch.{Patch => JPatch, Diff => JDiff, Operation => JOper}
import scala.jdk.CollectionConverters._


case class TextPatch(steps: List[TextPatch.Step]) extends Patch[String] {
  import TextPatch._

  private lazy val dmpPatches: ju.LinkedList[JPatch] = {
    val l = new ju.LinkedList[JPatch]()
    l.addAll(steps.map(Step.toDMP).asJava)

    l
  }

  override def isOpaque: Boolean = steps.isEmpty || steps.forall(_.events.isEmpty)

  override def apply(x: String): String = {
    val Array(res: String, _) = dmp.patchApply(dmpPatches, x)
    res
  }

  override def inverted: Patch[String] = TextPatch(steps = steps map { _.inverted })

  override def render(x: PatchVisitor): Unit = x.updateValue(this)

  override def toString: String = steps mkString "; "
}

object TextPatch {

  val Empty = TextPatch(Nil)

  final case class Step(
    start1: Int,
    start2: Int,
    len1: Int,
    len2: Int,
    events: List[Evt]) {

    def inverted: Step = Step(start1, start1, len1, len2, events map { _.inverted })

    override def toString: String = s"$start1/$start2 $len1/$len2${events.mkString(" "," ","")})"
  }

  final object Step {

    def apply(s1: Int, s2: Int, l1: Int, l2: Int, x: Evt, xs: Evt*): Step = Step(s1, s2, l1, l2, x +: xs.toList)

    def fromDMP(x: JPatch): Step = Step(
      x.start1,
      x.start2,
      x.length1,
      x.length2,
      x.diffs.asScala.map(Evt.fromDMP).toList)

    def toDMP(x: Step): JPatch = {
      val p = new JPatch
      p.start1 = x.start1
      p.start2 = x.start2
      p.length1 = x.len1
      p.length2 = x.len2
      p.diffs.addAll(x.events.map(Evt.toDMP).asJava)

      p
    }
  }

  sealed trait Evt {

    def inverted: Evt
  }
  final object Evt {

    final case class Equal(text: String) extends Evt {
      def inverted: Evt = this
      override def toString: String = s"Skip(${text.length})"
    }

    final case class Insert(text: String) extends Evt {
      def inverted: Evt = Delete(text)
    }

    final case class Delete(text: String) extends Evt {
      def inverted: Evt = Insert(text)
    }

    def fromDMP(x: JDiff): Evt = x.operation match {
      case JOper.DELETE => Delete(x.text)
      case JOper.EQUAL  => Equal(x.text)
      case JOper.INSERT => Insert(x.text)
    }

    def toDMP(x: Evt): JDiff = x match {
      case Delete(text) => new JDiff(JOper.DELETE, text)
      case Equal(text)  => new JDiff(JOper.EQUAL, text)
      case Insert(text) => new JDiff(JOper.INSERT, text)
    }
  }

  def make(l: String, r: String): TextPatch = TextPatch {
    dmp.patchMake(l, r).asScala.toList map { Step.fromDMP }
  }

  def apply(x: Step, xs: Step*): TextPatch = TextPatch(x +: xs.toList)
}