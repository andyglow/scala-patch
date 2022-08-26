package scalax.gpl.patch.macros

trait UPatchMakerDerivation extends UCommons {
  import c.universe._

  class Univer(ccName: TypeName, rootTpe: Type) {
    val T = new ConstantTypes(rootTpe)

    case class BundleTree(fields: List[PatchFieldTree]) {

      def tree: Tree = {
        val n  = fields.length
        val xs = fields flatMap { f =>
          List(f.nameAssignment, f.extractAssignment, f.pmAssignment)
        }
        val cn = TermName(s"Maker$n")
        q"""
          ${N.pkg}.ProductPatch.$cn(
            ${ccName.decodedName.toString},
            ..$xs,
            ${ccName.toTermName}.apply
          )
         """
      }
    }

    case class PatchFieldTree(
      idx: Int,
      name: TermName,
      tpe: Type
    ) {

      lazy val patchMakerType = appliedType(T.patchMaker.typeConstructor, tpe)
      lazy val patchMakerTree = c.inferImplicitValue(patchMakerType) orElse q"new ${T.names.purePatchMaker}[$tpe]()"

      lazy val nameAssignment    = {
        val n = TermName(s"_${idx}_name")
        q"$n = ${name.decodedName.toString}"
      }
      lazy val extractAssignment = {
        val n = TermName(s"_${idx}_extract")
        q"$n = _.$name"
      }
      lazy val pmAssignment      = {
        val n = TermName(s"_${idx}_patchMaker")
        q"$n = $patchMakerTree"
      }
    }

  }

  def derivePatchMaker(tpe: Type): Tree = {
    val T = new ConstantTypes(tpe)

    val tree = tpe match {

      case CaseClass(cc) =>
        val u   = new Univer(cc.name, tpe)
        val ast = u.BundleTree(cc.fields.toList.zipWithIndex map { case (f, i) => u.PatchFieldTree(i, f.name, f.tpe) })
        ast.tree

      case _ =>
        q"new ${T.purePatchMaker}[$tpe](): ${T.names.patchMaker}[$tpe]"
    }

    tree
  }
}
