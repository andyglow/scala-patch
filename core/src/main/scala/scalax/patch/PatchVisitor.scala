package scalax.patch

import java.io.{StringWriter, Writer}


trait PatchVisitor {

  def setValue(to: Any): Unit

  def unsetValue(from: Any): Unit

  def updateValue(from: Any, to: Any): Unit

  def increaseValue(delta: Any): Unit

  def decreaseValue(delta: Any): Unit

  def resize(sizeDelta: Int): Unit

  def addItems(xs: Seq[Any]): Unit

  def skipItems(n: Int): Unit

  def insertItems(xs: Seq[Any]): Unit

  def dropItems(xs: Seq[Any]): Unit

  def upgradeItems(xs: List[Patch[_]]): Unit

  def removeItems(xs: Seq[Any]): Unit

  def intoKey(key: Any): Unit

  def outofKey(key: Any): Unit

  def intoIndex(index: Int): Unit

  def outofIndex(index: Int): Unit

  def intoField(field: String): Unit

  def outofField(field: String): Unit
}

object PatchVisitor {

  case class Renderer(w: Writer, indent: Int = 2) extends PatchVisitor {

    private var indentation = ""

    private def nl(): Unit          = w.write("\n")
    private def writeIndent(): Unit = w.write(indentation)
    private def inc(): Unit         = indentation = indentation + " " * indent
    private def dec(): Unit         = indentation = indentation.dropRight(indent)

    override def setValue(to: Any): Unit = {
      writeIndent()
      w.write("set: to ")
      w.write(to.toString)
      nl()
    }

    override def unsetValue(from: Any): Unit = {
      writeIndent()
      w.write("unset: from ")
      w.write(from.toString)
      nl()
    }

    override def updateValue(from: Any, to: Any): Unit = {
      writeIndent()
      w.write("update: from ")
      w.write(from.toString)
      nl()
      writeIndent()
      w.write("      : to   ")
      w.write(to.toString)
      nl()
    }

    override def increaseValue(delta: Any): Unit = {
      writeIndent()
      w.write("increase ")
      w.write(delta.toString)
      nl()
    }

    override def decreaseValue(delta: Any): Unit = {
      writeIndent()
      w.write("decrease ")
      w.write(delta.toString)
      nl()
    }

    override def resize(delta: Int): Unit = {
      writeIndent()
      w.write("resize ")
      w.write(delta.toString)
      nl()
    }

    override def addItems(xs: Seq[Any]): Unit = {
      writeIndent()
      w.write("add")
      nl()
      xs foreach { x =>
        writeIndent()
        w.write("+ ")
        w.write(x.toString)
        nl()
      }
    }

    override def removeItems(xs: Seq[Any]): Unit = {
      writeIndent()
      w.write("remove")
      nl()
      xs foreach { x =>
        writeIndent()
        w.write("- ")
        w.write(x.toString)
        nl()
      }
    }

    def skipItems(n: Int): Unit = {
      writeIndent()
      w.write(s"skip $n")
      nl()
    }

    def insertItems(xs: Seq[Any]): Unit = {
      writeIndent()
      w.write("insert")
      nl()
      xs foreach { x =>
        writeIndent()
        w.write("- ")
        w.write(x.toString)
        nl()
      }
    }

    def dropItems(xs: Seq[Any]): Unit = {
      writeIndent()
      w.write(s"drop ${xs.length}")
      nl()
    }

    def upgradeItems(xs: List[Patch[_]]): Unit = {
      writeIndent()
      w.write("upgrade")
      nl()
      xs foreach { patch =>
        writeIndent()
        w.write("- ")
        patch.render(this)
      }
    }

    override def intoKey(key: Any): Unit = {
      writeIndent()
      w.write("key '")
      w.write(key.toString)
      w.write("' {")
      nl()
      inc()
    }

    override def outofKey(key: Any): Unit = {
      dec()
      writeIndent()
      w.write("}")
      nl()
    }

    override def intoIndex(index: Int): Unit = {
      writeIndent()
      w.write("index ")
      w.write(index.toString)
      w.write(" {")
      nl()
      inc()
    }

    override def outofIndex(index: Int): Unit = {
      dec()
      writeIndent()
      w.write("}")
      nl()
    }

    override def intoField(field: String): Unit = {
      writeIndent()
      w.write("field '")
      w.write(field)
      w.write("' {")
      nl()
      inc()
    }

    override def outofField(field: String): Unit = {
      dec()
      writeIndent()
      w.write("}")
      nl()
    }
  }

  def render[T](w: Writer, patch: Patch[T]): Unit = patch.render(Renderer(w))

  def stringify[T](patch: Patch[T]): String = {
    val w = new StringWriter
    render(w, patch)
    w.toString
  }
}