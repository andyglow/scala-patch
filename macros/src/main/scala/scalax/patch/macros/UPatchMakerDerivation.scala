package scalax.patch.macros

import scalax.patch.PatchMaker

trait UPatchMakerDerivation extends UCommons {
  import c.universe._

  def derivePatchMaker(tpe: Type): Tree = {

    val patchTpe = appliedType(patch, tpe)
    val patchTermName = patchTpe.typeSymbol.name.toTermName

    val constantPatchMakerTpe = appliedType(constantPatchMaker, tpe)
    val constantPatchMakerTermName = constantPatchMakerTpe.typeSymbol.name.toTermName

    val abstractPatchMakerTpe = appliedType(abstractPatchMaker, tpe)
    val abstractPatchMakerTypeName = abstractPatchMakerTpe.typeSymbol.name.toTypeName
    val abstractPatchMakerTermName = abstractPatchMakerTpe.typeSymbol.name.toTermName

    val patchMakerTpe = appliedType(patchMaker, tpe)
    val patchMakerTermName = patchMakerTpe.typeSymbol.name.toTermName

    val patchVisitorTypeName = patchVisitor.typeSymbol.name.toTypeName

    val tree = tpe match {

      case CaseClass(cc) =>
        val patchClassTypeName = TypeName(s"$$${cc.name}$$Patch")
        val patchMakerClassTypeName = TypeName(s"$$${cc.name}$$PatchMaker")

        val patchClassParams = cc.fields map { f =>
          ValDef(Modifiers(), f.name, TypeTree(appliedType(patch, f.tpe)), EmptyTree)
        }

        val nestedPatchMakers = cc.fields.map { f =>
          val nestedPatchMakerType = appliedType(patchMakerTpe, f.tpe)
          val nestedPatchMakerTree = c.inferImplicitValue(nestedPatchMakerType)
          (f.name, nestedPatchMakerTree)
        }.toMap

        val patchClassApplyMethodBody = {
          val assign = cc.fields map { f =>
            q"${f.name} = this.${f.name}(x.${f.name})"
          }

          q"${cc.name.toTermName}(..$assign)"
        }

        val patchClassIsOpaqueMethodBody = cc.fields.foldLeft(EmptyTree) {
          case (EmptyTree, f) => q"${f.name}.isOpaque"
          case (agg, f)       => q"($agg) && ${f.name}.isOpaque"
        }

        val patchClassInvertedMethodBody = {
          val assign = cc.fields map { f =>
            q"${f.name} = ${f.name}.inverted"
          }

          q"${patchClassTypeName.toTermName}(..$assign)"
        }

        val patchClassVisitMethodBody = {
          val renderFields = cc.fields.toList map { f =>
            val name = f.name.encodedName.toString
            q"""
               if (this.${f.name}.nonOpaque) {
                 x.intoField($name)
                 this.${f.name}.visit(x)
                 x.outofField($name)
               }
               """
          }
          renderFields.lastOption match {
            case Some(last) => Block.apply(renderFields.dropRight(1), last)
            case None       => EmptyTree
          }
        }

        val patchClassToStringMethodBody = {
          val eq = "="
          val sep = ", "

          cc.fields.map { f =>
            q"${f.name.decodedName.toString} + $eq + ${f.name}.toString"
          }.foldLeft(EmptyTree) {
            case (EmptyTree, f) => f
            case (agg, f)       => q"$agg + $sep + $f"
          }
        }

        val patchMakerClassToStringMethodBody = {
          val eq = "="
          val sep = ", "

          cc.fields.map { f =>
            val nestedPatchMaker = nestedPatchMakers(f.name)
            q"${f.name.decodedName.toString} + $eq + ${nestedPatchMaker}.toString"
          }.foldLeft(EmptyTree) {
            case (EmptyTree, f) => f
            case (agg, f)       => q"$agg + $sep + $f"
          }
        }

        val patchClassMKBody = {
          val assign = cc.fields map { f =>
            val nestedPatchMaker = nestedPatchMakers(f.name)
            q"${f.name} = $patchTermName.make(l.${f.name}, r.${f.name})($nestedPatchMaker)"
          }

          q"${patchClassTypeName.toTermName}(..$assign)"
        }

        q"""
          case class $patchClassTypeName(..$patchClassParams) extends ${patchTermName.toTypeName}[$tpe] {
            def apply(x: ${cc.tpe}): ${cc.tpe}          = $patchClassApplyMethodBody
            def isOpaque: Boolean                       = $patchClassIsOpaqueMethodBody
            def inverted: $patchTpe                     = $patchClassInvertedMethodBody
            def visit(x: $patchVisitorTypeName): Unit   = $patchClassVisitMethodBody
            override def toString: String               = ${patchClassTypeName.decodedName.toString} + "(" + $patchClassToStringMethodBody + ")"
          }

          case class $patchMakerClassTypeName() extends ${patchMakerTermName}.${abstractPatchMakerTypeName}[$tpe] (
            $patchMakerTermName.Kind.Structure(${cc.name.decodedName.toString}),
            { case (l, r) => $patchClassMKBody }
          ) {
            override def toString: String = ${patchMakerClassTypeName.decodedName.toString} + "(" + $patchMakerClassToStringMethodBody + ")"
          }

          new $patchMakerClassTypeName()
          """

      case _ =>
        q"new $constantPatchMakerTpe[$tpe]()"
    }

    tree
  }
}
