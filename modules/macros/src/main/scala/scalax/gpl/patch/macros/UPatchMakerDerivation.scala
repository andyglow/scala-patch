package scalax.gpl.patch.macros

import scala.util.Random

trait UPatchMakerDerivation extends UCommons {
  import c.universe._
  private val rand = new Random()

  class Univer(ccName: TypeName, rootTpe: Type) {
    val suffix = rand.alphanumeric.take(4).mkString

    val T                                           = new ConstantTypes(rootTpe)
    val ccPatchTypeName: TypeName                   = TypeName(s"${ccName}_${suffix}_Patch")
    val ccPatchMakerTypeName: TypeName              = TypeName(s"${ccName}_${suffix}_PatchMaker")
    val ccNestedPatchMakersHolderTypeName: TypeName = TypeName(s"${ccName}_${suffix}_mPatchMakerHolder")

    case class BundleTree(fields: List[PatchFieldTree]) {

      object patch {
        // Patch Constructor Parameters
        def ctorParams: Seq[Tree] = fields.map(_.patchField)

        // Patch Apply Method Body
        def applyBody: Tree = {
          val assign = fields map { f =>
            q"${f.name} = this.${f.name}(x.${f.name})"
          }

          q"${ccName.toTermName}(..$assign)"
        }

        // Patch IsOpaque Method Body
        def isOpaqueBody: Tree = fields.foldLeft(EmptyTree) {
          case (EmptyTree, f) => q"${f.name}.isOpaque"
          case (agg, f)       => q"($agg) && ${f.name}.isOpaque"
        }

        // Patch Inverted Method Body
        def invertedBody: Tree = {
          val assign = fields map { f =>
            q"${f.name} = ${f.name}.inverted"
          }

          q"${ccPatchTypeName.toTermName}(..$assign)"
        }

        // Patch Visit Method Body
        def visitBody: Tree = {
          val renderFields = fields map { f =>
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

        // Patch ToString Method Body
        def toStringBody: Tree = {
          val eq  = "="
          val sep = ", "

          val details = fields.map { f =>
            q"${f.name.decodedName.toString} + $eq + ${f.name}.toString"
          }.foldLeft(EmptyTree) {
            case (EmptyTree, f) => f
            case (agg, f)       => q"$agg + $sep + $f"
          }

          q"""${ccPatchTypeName.decodedName.toString} + "(" + $details + ")""""
        }

        def tree: Tree =
          q"""
          case class $ccPatchTypeName(..$ctorParams) extends ${N.pkg}.${T.names.patch}[$rootTpe] {
            def apply(x: $rootTpe): $rootTpe      = $applyBody
            def isOpaque: Boolean                 = $isOpaqueBody
            def inverted: ${T.patchApplied}       = $invertedBody
            def visit(x: ${T.patchVisitor}): Unit = $visitBody
            override def toString(): String       = $toStringBody
          }
           """
      }

      object patchMaker {

        // PatchMaker Make Function Body
        def makeBody: Tree = {
          val assign = fields map { f =>
            q"${f.name} = ${f.patchMakerAddress}.make(l.${f.name}, r.${f.name})"
          }

          q"${ccPatchTypeName.toTermName}(..$assign)"
        }

        // PatchMaker ToString Method Body
        val toStringBody = {
          val eq  = "="
          val sep = ", "

          val fieldsToString = fields.map { f =>
            q"${f.name.decodedName.toString} + $eq + ${f.patchMakerAddress}.toString"
          }.foldLeft(EmptyTree) {
            case (EmptyTree, f) => f
            case (agg, f)       => q"$agg + $sep + $f"
          }

          q"""${ccPatchMakerTypeName.decodedName.toString} + "(" + $fieldsToString + ")""""
        }

        def tree: Tree =
          q"""
            case class $ccPatchMakerTypeName() extends ${N.PatchMaker}.${T.names.abstractPatchMaker}[$rootTpe] (
              ${N.PatchMaker}.Kind.Structure(${ccName.decodedName.toString}),
              { case (l, r) => $makeBody }
            ) {
              override def toString(): String = $toStringBody
            }
           """
      }

      object patchMakerHolder {

        def tree: Tree =
          q"""
            object ${ccNestedPatchMakersHolderTypeName.toTermName} {
              ..${fields.map(_.patchMakerValDef)}
            }
           """
      }

      def tree: Tree =
        q"""
          ${patch.tree}

          ${patchMaker.tree}

          ${patchMakerHolder.tree}

          new $ccPatchMakerTypeName(): ${T.names.patchMaker}[$rootTpe]
         """
    }

    case class PatchFieldTree(
      name: TermName,
      tpe: Type
    ) {
      lazy val patchType  = appliedType(T.patch.typeConstructor, tpe)
      lazy val patchField = ValDef(Modifiers(), name, TypeTree(patchType), EmptyTree)

      lazy val patchMakerType    = appliedType(T.patchMaker.typeConstructor, tpe)
      lazy val patchMakerTree    = c.inferImplicitValue(patchMakerType) orElse q"new ${T.names.purePatchMaker}[$tpe]()"
      lazy val patchMakerName    = TermName("$" + name + "$pm")
      lazy val patchMakerValDef  = q"lazy val $patchMakerName: ${T.names.patchMaker}[$tpe] = $patchMakerTree"
      lazy val patchMakerAddress = q"${ccNestedPatchMakersHolderTypeName.toTermName}.$patchMakerName"
    }

  }

  def derivePatchMaker(tpe: Type): Tree = {
    val T = new ConstantTypes(tpe)

    val tree = tpe match {

      case CaseClass(cc) =>
        val u   = new Univer(cc.name, tpe)
        val ast = u.BundleTree(cc.fields.toList map { f => u.PatchFieldTree(f.name, f.tpe) })
        ast.tree

      case _ =>
        q"new ${T.purePatchMaker}[$tpe](): ${T.names.patchMaker}[$tpe]"
    }

    tree
  }
}
