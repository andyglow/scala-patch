package scalax.patch.macros

import scalax.patch.PatchMaker

import scala.reflect.macros.blackbox


class PatchMakerMacro(val c: blackbox.Context) extends UCommons {
  import c.universe._

  def deriveImpl[T](implicit t: c.WeakTypeTag[T]): c.Expr[PatchMaker[T]] = {
    val tpe = t.tpe

    val patchTpe             = appliedType(patch, tpe)
    val patchTermName        = patchTpe.typeSymbol.name.toTermName
    val patchMakerTpe        = appliedType(patchMaker, tpe)
    val patchMakerTermName   = patchMakerTpe.typeSymbol.name.toTermName
    val patchVisitorTypeName = patchVisitor.typeSymbol.name.toTypeName

    val tree = tpe match {

      case CaseClass(cc) =>
        val pcn = TypeName(s"$$${cc.name}$$Patch")

        val params = cc.fields map { f =>
          ValDef(Modifiers(), f.name, TypeTree(appliedType(patch, f.tpe)), EmptyTree)
        }

        val applyBody = {
          val assign = cc.fields map { f =>
            q"${f.name} = this.${f.name}(x.${f.name})"
          }

          q"${cc.name.toTermName}(..$assign)"
        }

        val isOpaqueBody = cc.fields.foldLeft(EmptyTree) {
          case (EmptyTree, f) => q"${f.name}.isOpaque"
          case (agg, f)       => q"($agg) && ${f.name}.isOpaque"
        }

        val invertedBody = {
          val assign = cc.fields map { f =>
            q"${f.name} = ${f.name}.inverted"
          }

          q"${pcn.toTermName}(..$assign)"
        }

        val renderBody = {
//          val cls = q"classOf[${cc.tpe}]"
          val renderFields = cc.fields.toList map { f =>
            val name = f.name.encodedName.toString
            q"""
               x.intoField($name)
               this.${f.name}.render(x)
               x.outofField($name)
               """
          }
          renderFields.lastOption match {
            case Some(last) => Block.apply(renderFields.dropRight(1), last)
            case None       => EmptyTree
          }
        }

        val mkBody = {
          val assign = cc.fields map { f =>
            q"${f.name} = $patchTermName.make(l.${f.name}, r.${f.name})"
          }

          q"${pcn.toTermName}(..$assign)"
        }

        q"""
          case class $pcn(..$params) extends ${patchTermName.toTypeName}[$tpe] {
            def apply(x: ${cc.tpe}): ${cc.tpe}          = $applyBody
            def isOpaque: Boolean                       = $isOpaqueBody
            def inverted: $patchTpe                     = $invertedBody
            def render(x: $patchVisitorTypeName): Unit  = $renderBody
          }

          $patchMakerTermName.mk[${cc.tpe}]($patchMakerTermName.Kind.Structure) { case (l, r) => $mkBody }
          """

      case _ =>
        q"""$patchMakerTermName.mk[$tpe]($patchMakerTermName.Kind.Constant) {
           case (null, r) => $patchTermName.SetValue(r)
           case (l, null) => $patchTermName.UnsetValue(l)
           case (l, r)    => $patchTermName.UpdateValue(l, r)
        }"""
    }

    if(c.settings.contains("print-patch-maker-code")) info(showCode(tree))

    c.Expr[PatchMaker[T]](tree)
  }
}