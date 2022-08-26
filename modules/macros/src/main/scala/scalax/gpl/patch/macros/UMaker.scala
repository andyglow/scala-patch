package scalax.gpl.patch.macros

private[macros] trait UMaker { this: UCommons =>
  import c.universe._

  def makeImpl[T](tpe: Type, params: ParameterMap): Expr[T] = {
    val T      = new ConstantTypes(tpe)
    def defIns = c.inferImplicitValue(T.defaultApplied)
    val cc     = CaseClass.unapply(tpe) getOrElse {
      err("must be case class")
    }

    c.Expr[T](cc.newInstanceTree(params, defIns))
  }
}
