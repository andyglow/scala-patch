package scalax.patch.macros

import scala.reflect.NameTransformer
import scala.util.control.NonFatal

//import scala.reflect.NameTransformer


private[macros] trait Extractors { this: HasLog with HasContext =>
  import c.universe._

  val optionTpe: Type = weakTypeOf[Option[_]]

  def resolveGenericType(x: Type, from: List[Symbol], to: List[Type]): Type = {
    try x.substituteTypes(from, to) catch {
      case NonFatal(_) =>
        c.abort(
          c.enclosingPosition,
          s"""Cannot resolve generic type(s) for `$x`
             |Please provide a custom implicitly accessible json.Schema for it.
             |""".stripMargin)
    }
  }

  final case class CaseClass(
    name: TypeName,
    tpe: Type,
    fields: Seq[CaseClass.Field])

  final object CaseClass {

    // TODO: add support for case classes defined in method body

    final def lookupCompanionOf(clazz: Symbol): Symbol = clazz.companion

    def possibleApplyMethodsOf(subjectCompanion: Symbol): List[MethodSymbol] = {
      val subjectCompanionTpe = subjectCompanion.typeSignature

      subjectCompanionTpe.decl(TermName("apply")) match {

        case NoSymbol =>
          c.abort(c.enclosingPosition, s"No apply function found for ${subjectCompanion.fullName}")

        case x => x.asTerm.alternatives flatMap { apply =>
          val method = apply.asMethod

          def areAllImplicit(pss: List[List[Symbol]]): Boolean = pss forall {
            case p :: _ => p.isImplicit
            case _      => false
          }

          method.paramLists match {
            case ps :: pss if ps.nonEmpty && areAllImplicit(pss) => List(method)
            case _ => List.empty
          }
        }
      }
    }

    def applyMethod(subjectCompanion: Symbol): Option[MethodSymbol] =
      possibleApplyMethodsOf(subjectCompanion).headOption

    case class Field(
      name: TermName,
      tpe: Type,
      effectiveTpe: Type,
      annotations: List[Annotation],
      default: Option[Tree],
      isOption: Boolean) {

      def hasDefault: Boolean = default.isDefined
    }

    def fieldMap(tpe: Type): Seq[Field] = {

      val annotationMap = tpe.decls.collect {

        case s: MethodSymbol if s.isCaseAccessor =>
          // workaround: force loading annotations
          s.typeSignature
          s.accessed.annotations.foreach(_.tree.tpe)

          s.name.toString.trim -> s.accessed.annotations
      }.toMap

      val subjectCompanionSym = tpe.typeSymbol
      val subjectCompanion    = lookupCompanionOf(subjectCompanionSym)

      def toField(fieldSym: TermSymbol, i: Int): Field = {
        val name        = NameTransformer.decode(fieldSym.name.toString)
        val fieldTpe    = fieldSym.typeSignature.dealias // In(tpe).dealias
        val isOption    = fieldTpe <:< optionTpe
        val hasDefault  = fieldSym.isParamWithDefault
        val default     = if (hasDefault) {
          val getter = TermName("apply$default$" + (i + 1))
          Some(q"$subjectCompanion.$getter")
        } else
          None

        def effectiveType = if (tpe.typeArgs.nonEmpty && tpe.typeSymbol.isClass) {
          resolveGenericType(
            fieldTpe,
            tpe.typeSymbol.asClass.typeParams,
            tpe.typeArgs)
        } else
          fieldTpe

        val specifiedType =
          if (isOption) effectiveType.typeArgs.head
          else
            effectiveType

        Field(
          name          = TermName(name),
          tpe           = fieldTpe,
          effectiveTpe  = specifiedType,
          annotations   = annotationMap.getOrElse(name, List.empty),
          default       = default,
          isOption      = isOption)
      }

      applyMethod(subjectCompanion)

      val fields = tpe.typeSymbol.asClass.primaryConstructor.asMethod.paramLists.headOption map { params =>
        params.map { _.asTerm }.zipWithIndex map { case (f, i) => toField(f, i) }
      }

      fields getOrElse Seq.empty
    }

    def unapply(tpe: Type): Option[CaseClass] = {
      val symbol = tpe.typeSymbol

      if (symbol.isClass) {
        val clazz = symbol.asClass
        if (clazz.isCaseClass) {
          if (clazz.isDerivedValueClass) None else Some(CaseClass(symbol.name.toTypeName, tpe, fieldMap(tpe)))
        } else
          None
      } else
        None
    }
  }
}
