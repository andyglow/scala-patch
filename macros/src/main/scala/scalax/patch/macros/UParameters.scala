package scalax.patch.macros

private[macros] trait UParameters { this: ULogging with UContext with UProductTypes =>
  import c.universe._

  sealed trait Parameter

  object Parameter {

    case class ByName(value: String) extends Parameter
    object ByName {

      def fromTree(x: Tree): ByName = {
        x match {
          // when "name"
          case Literal(Constant(x: String)) => ByName(x)
          // when 'name
          case Apply(
                Select(Select(Ident(TermName("scala")), TermName("Symbol")), TermName("apply")),
                List(Literal(Constant(x: String)))
              ) =>
            ByName(x)
          // otherwise
          case _                            => err(s"can't take parameter name from: ${show(x)}")
        }
      }
    }

    case class ByIndex(value: Int) extends Parameter
  }

  sealed trait TreeWithSource                                                { def tree: Tree }
  object TreeWithSource                                                      {
    case class Direct(tree: Tree)                   extends TreeWithSource
    case class FromCaseClass(tpe: Type, tree: Tree) extends TreeWithSource
  }
  class ParameterMap(val parameters: Seq[(Parameter, List[TreeWithSource])]) {

    def find(pred: Parameter => Boolean): Option[List[TreeWithSource]] = {
      parameters collectFirst { case (p, trees) if pred(p) => trees }
    }

    def findByName(x: String): Option[List[TreeWithSource]] = find {
      case Parameter.ByName(`x`) => true
      case _                     => false
    }

    def findByIndex(x: Int): Option[List[TreeWithSource]] = find {
      case Parameter.ByIndex(`x`) => true
      case _                      => false
    }

    def findTrees(name: String, idx: Int): Seq[TreeWithSource] = {
      parameters.collect {
        case (Parameter.ByName(`name`), trees) => trees
        case (Parameter.ByIndex(`idx`), trees) => trees
      }.flatten
    }

    def append(thatP: Parameter, thatT: List[TreeWithSource]): ParameterMap = {
      var existing = false
      val entries  = parameters map { case (thisP, thisT) =>
        if (thisP == thatP) {
          existing = true
          (thisP, thisT ++ thatT)
        } else
          (thisP, thisT)
      }

      val params = if (existing) entries else entries :+ ((thatP, thatT))

      new ParameterMap(params)
    }

    def ++(that: ParameterMap): ParameterMap = {
      that.parameters.foldLeft(this) { case (acc, (p, t)) =>
        acc.append(p, t)
      }
    }
  }

  class ParameterMapExtractor {

    def fromCaseClassDefinition(tree: Tree, cc: CaseClass): ParameterMap = {
      val params = cc.fields.map { f =>
        Parameter.ByName(f.name.decodedName.toString) -> List(
          TreeWithSource.FromCaseClass(cc.tpe, c.typecheck(q"$tree.${f.name}"))
        )
      }

      new ParameterMap(params)
    }

    def fromCall(xs: Tree*): ParameterMap = {
      val params = xs.zipWithIndex map {
        // case "key" -> "val" // scala 2.12
        case (
              Apply(
                TypeApply(
                  Select(
                    Apply(
                      TypeApply(
                        Select(Select(Ident(TermName("scala")), TermName("Predef")), TermName("ArrowAssoc")),
                        List(TypeTree())
                      ),
                      List(k)
                    ),
                    TermName("$minus$greater")
                  ),
                  List(TypeTree())
                ),
                List(v)
              ),
              _
            ) =>
          (Parameter.ByName fromTree k, List(TreeWithSource.Direct(v)))
        // case "key" -> "val" // scala 2.11
        case (
              Apply(
                TypeApply(
                  Select(
                    Apply(
                      TypeApply(
                        Select(Select(This(TypeName("scala")), TermName("Predef")), TermName("ArrowAssoc")),
                        List(TypeTree())
                      ),
                      List(k)
                    ),
                    TermName("$minus$greater")
                  ),
                  List(TypeTree())
                ),
                List(v)
              ),
              _
            ) =>
          (Parameter.ByName fromTree k, List(TreeWithSource.Direct(v)))
        // case ("key", "val")
        case (
              Apply(
                TypeApply(
                  Select(Select(Ident(TermName("scala")), TermName("Tuple2")), TermName("apply")),
                  List(TypeTree(), TypeTree())
                ),
                List(k, v)
              ),
              _
            ) =>
          (Parameter.ByName fromTree k, List(TreeWithSource.Direct(v)))
        // case "val"
        case (v, idx) => (Parameter.ByIndex(idx), List(TreeWithSource.Direct(v)))
      }

      new ParameterMap(params)
    }
  }

  val ParameterMap = new ParameterMapExtractor

}
