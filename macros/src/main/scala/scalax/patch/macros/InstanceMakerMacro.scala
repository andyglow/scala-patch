package scalax.patch.macros

import scala.reflect.macros.blackbox

class InstanceMakerMacro(val c: blackbox.Context) extends UCommons {
  import c.universe._

  private def cc(tpe: Type) = CaseClass.getOrErr(tpe, t => s"must be case class: ${show(t)}")

  def partialImpl1[T: WeakTypeTag, F1: WeakTypeTag](f1: c.Tree): c.Expr[T] = {
    val target = cc(weakTypeOf[T])
    val from1  = cc(weakTypeOf[F1])

    val params = ParameterMap.fromCaseClassDefinition(f1, from1)
    c.Expr[T](target.newInstanceTree(params, EmptyTree))
  }

  def partialImpl2[T: WeakTypeTag, F1: WeakTypeTag, F2: WeakTypeTag](f1: c.Tree, f2: c.Tree): c.Expr[T] = {
    val target = cc(weakTypeOf[T])
    val from1  = cc(weakTypeOf[F1])
    val from2  = cc(weakTypeOf[F2])

    val params = ParameterMap.fromCaseClassDefinition(f1, from1) ++
      ParameterMap.fromCaseClassDefinition(f2, from2)
    c.Expr[T](target.newInstanceTree(params, EmptyTree))
  }

  def partialImpl3[T: WeakTypeTag, F1: WeakTypeTag, F2: WeakTypeTag, F3: WeakTypeTag](
    f1: c.Tree,
    f2: c.Tree,
    f3: c.Tree
  ): c.Expr[T] = {
    val target = cc(weakTypeOf[T])
    val from1  = cc(weakTypeOf[F1])
    val from2  = cc(weakTypeOf[F2])
    val from3  = cc(weakTypeOf[F3])

    val params = ParameterMap.fromCaseClassDefinition(f1, from1) ++
      ParameterMap.fromCaseClassDefinition(f2, from2) ++
      ParameterMap.fromCaseClassDefinition(f3, from3)

    c.Expr[T](target.newInstanceTree(params, EmptyTree))
  }

  def partialImpl4[T: WeakTypeTag, F1: WeakTypeTag, F2: WeakTypeTag, F3: WeakTypeTag, F4: WeakTypeTag](
    f1: c.Tree,
    f2: c.Tree,
    f3: c.Tree,
    f4: c.Tree
  ): c.Expr[T] = {
    val target = cc(weakTypeOf[T])
    val from1  = cc(weakTypeOf[F1])
    val from2  = cc(weakTypeOf[F2])
    val from3  = cc(weakTypeOf[F3])
    val from4  = cc(weakTypeOf[F4])

    val params = ParameterMap.fromCaseClassDefinition(f1, from1) ++
      ParameterMap.fromCaseClassDefinition(f2, from2) ++
      ParameterMap.fromCaseClassDefinition(f3, from3) ++
      ParameterMap.fromCaseClassDefinition(f4, from4)

    c.Expr[T](target.newInstanceTree(params, EmptyTree))
  }

  def partialImpl5[T: WeakTypeTag, F1: WeakTypeTag, F2: WeakTypeTag, F3: WeakTypeTag, F4: WeakTypeTag, F5: WeakTypeTag](
    f1: c.Tree,
    f2: c.Tree,
    f3: c.Tree,
    f4: c.Tree,
    f5: c.Tree
  ): c.Expr[T] = {
    val target = cc(weakTypeOf[T])
    val from1  = cc(weakTypeOf[F1])
    val from2  = cc(weakTypeOf[F2])
    val from3  = cc(weakTypeOf[F3])
    val from4  = cc(weakTypeOf[F4])
    val from5  = cc(weakTypeOf[F5])

    val params = ParameterMap.fromCaseClassDefinition(f1, from1) ++
      ParameterMap.fromCaseClassDefinition(f2, from2) ++
      ParameterMap.fromCaseClassDefinition(f3, from3) ++
      ParameterMap.fromCaseClassDefinition(f4, from4) ++
      ParameterMap.fromCaseClassDefinition(f5, from5)

    c.Expr[T](target.newInstanceTree(params, EmptyTree))
  }

  def deriveDynamic[T](name: c.Expr[String])(xs: c.Tree*)(implicit t: c.WeakTypeTag[T]): c.Expr[T] = {
    name.tree match {
      case Literal(Constant("apply")) =>
      case illegalName                => err(s"only 'apply' method is supported: ${show(illegalName)}")
    }

    makeImpl[T](weakTypeOf[T], ParameterMap.fromCall(xs: _*))
  }

  def deriveImpl[T](xs: c.Tree*)(implicit t: c.WeakTypeTag[T]): c.Expr[T] = {
    makeImpl[T](weakTypeOf[T], ParameterMap.fromCall(xs: _*))
  }
}
