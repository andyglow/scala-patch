package scalax.gpl.patch.macros

import scalax.Default
import scalax.gpl.patch.PatchMaker.{AbstractPatchMaker, PurePatchMaker}
import scalax.gpl.patch._

private[macros] trait UCommons extends UProductTypes with ULogging with UContext with UParameters with UMaker {
  import c.universe._

  def suffixName(x: Tree): Name = x match {
    case Select(_, x) => x
    case _            => err(s"Unexpected tree ${showRaw(x)}. Expected Select(...)")
  }

  private[macros] class ConstantNames {
    val pkg = q"_root_.scalax.gpl.patch"
    val Patch = q"$pkg.Patch"
    val PatchMaker = q"$pkg.PatchMaker"
    val AbstractPatchMaker = q"$pkg.PatchMaker.AbstractPatchMaker"
    val PatchVisitor = q"$pkg.PatchVisitor"
  }

  private[macros] val N = new ConstantNames

  private[macros] class ConstantTypes(param: Type) {
    val patch = typeOf[Patch[_]]
    val patchApplied = appliedType(patch.typeConstructor, param)
    val patchMaker = typeOf[PatchMaker[_]]
    val abstractPatchMaker = typeOf[AbstractPatchMaker[_]]
    val purePatchMaker = typeOf[PurePatchMaker[_]]
    val patchVisitor = typeOf[PatchVisitor]
    val default = typeOf[Default[_]]
    val defaultApplied = appliedType(default.typeConstructor, param)

    lazy val names = new ConstantTypeNames(this)
  }

  private[macros] class ConstantTypeNames(T: ConstantTypes) {
    val patch = T.patch.typeSymbol.asType.name // .toTypeName
    val patchMaker = T.patchMaker.typeSymbol.asType.name // .toTypeName
    val purePatchMaker = T.purePatchMaker.typeSymbol.asType.name // .toTypeName
    val abstractPatchMaker = T.abstractPatchMaker.typeSymbol.asType.name // .toTypeName
  }

}
