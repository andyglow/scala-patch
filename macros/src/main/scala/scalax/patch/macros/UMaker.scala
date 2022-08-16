package scalax.patch.macros

private[macros] trait UMaker { this: UCommons =>
  import c.universe._

  def makeImpl[T](tpe: Type, params: ParameterMap): Expr[T] = {
    def defIns = c.inferImplicitValue(appliedType(default, tpe))
    val cc     = CaseClass.unapply(tpe) getOrElse {
      err("must be case class")
    }

    c.Expr[T](cc.newInstanceTree(params, defIns))
  }
}
