package scalax.gpl.scalatest

import scalax.patch.{Patch, PatchMaker, PatchVisitor}

import scala.language.implicitConversions

case class MismatchReport(
  mismatches: List[(MismatchReport.Branch, String)]
) {

  override def toString: String = mismatches.map { case (branch, message) => s"- $branch: $message" }.mkString("\n")
}

object MismatchReport {

  def compute[T: PatchMaker](l: T, r: T): MismatchReport = {
    val patch = Patch.make(l, r)
    compute(patch)
  }

  def compute[T](patch: Patch[T]): MismatchReport = {
    import BranchElement._

    val branch                       = new MutableBranch()
    branch.append(BranchElement.Root)
    def push(x: BranchElement): Unit = branch.append(x)
    def pop(): Unit                  = branch.shrink()

    var mismatches                   = List.empty[(Branch, String)]
    def newMismatch(x: String): Unit = mismatches = mismatches :+ ((branch.copy(), x))

    patch.visit(new PatchVisitor {
      override def setValue(to: Any): Unit                = newMismatch(s"expected: $to, actual: null")
      override def unsetValue(from: Any): Unit            = newMismatch(s"expected: null, actual: $from")
      override def updateValue(from: Any, to: Any): Unit  = newMismatch(s"expected: $to, actual: $from")
      override def updateValue(update: Any): Unit         = newMismatch(s"expected: $update")
      override def increaseValue(delta: Any): Unit        = newMismatch(s"expected value is $delta bigger then actual")
      override def decreaseValue(delta: Any): Unit        = newMismatch(s"expected value is $delta less then actual")
      override def resize(sizeDelta: Int): Unit           = ()
      override def addItems(xs: Seq[Any]): Unit           = newMismatch(s"expected items not found: ${xs.mkString(",")}")
      override def skipItems(n: Int): Unit                = ()
      override def insertItems(xs: Seq[Any]): Unit        = newMismatch(s"expected items not found: ${xs.mkString(",")}")
      override def dropItems(xs: Seq[Any]): Unit          = newMismatch(s"excessive items detected: ${xs.mkString(",")}")
      override def upgradeItems(xs: List[Patch[_]]): Unit = newMismatch(s"non-equal items found: ${xs.mkString(",")}")
      override def removeItems(xs: Seq[Any]): Unit        = newMismatch(s"excessive items detected: ${xs.mkString(",")}")
      override def intoKey(key: Any): Unit                = push(MapKey(key))
      override def outofKey(key: Any): Unit               = pop()
      override def intoIndex(index: Int): Unit            = push(CollectionIndex(index))
      override def outofIndex(index: Int): Unit           = pop()
      override def intoField(field: String): Unit         = push(StructField(field))
      override def outofField(field: String): Unit        = pop()
    })

    MismatchReport(mismatches)
  }

  case class Branch(xs: BranchElement*) {

    override def toString: String = {
      import BranchElement._

      var sb = new StringBuilder
      xs foreach {
        case BranchElement.Root   => sb = new StringBuilder("root")
        case StructField(name)    => sb.append(".").append(name)
        case MapKey(key)          => sb.append(".").append(key)
        case CollectionIndex(key) => sb.append("[").append(key).append("]")
      }
      sb.toString
    }
  }

  class MutableBranch() {
    private[scalatest] var elements = List.empty[BranchElement]

    def append(y: BranchElement): Unit = elements = elements :+ y

    def shrink(): Unit = elements = elements.dropRight(1)

    def copy(): Branch = Branch(elements.toSeq: _*)
  }

  sealed trait BranchElement

  object BranchElement {

    case object Root extends BranchElement

    case class StructField(name: String) extends BranchElement

    case class CollectionIndex(value: Int) extends BranchElement

    case class MapKey(value: Any) extends BranchElement

    implicit def nameToElement(x: String): StructField   = StructField(x)
    implicit def indexToElement(x: Int): CollectionIndex = CollectionIndex(x)
    implicit def keyToElement(x: Any): MapKey            = MapKey(x)
  }

  val Root = BranchElement.Root
}
