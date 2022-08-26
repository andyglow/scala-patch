package scalax

import scalax.gpl.patch.macros.InstanceMakerMacro
import scala.language.dynamics

object make extends Dynamic {

  def applyDynamic[T](name: String)(xs: Any*): T = macro InstanceMakerMacro.deriveDynamic[T]
  def applyDynamicNamed[T](name: String)(xs: (String, Any)*): T = macro InstanceMakerMacro.deriveDynamic[T]

  object from {
    def apply[T](xs: Any*): T = macro InstanceMakerMacro.deriveImpl[T]

    class partial[T] {
      def apply[F1](f1: F1): T = macro InstanceMakerMacro.partialImpl1[T, F1]
      def apply[F1, F2](f1: F1, f2: F2): T = macro InstanceMakerMacro.partialImpl2[T, F1, F2]
      def apply[F1, F2, F3](f1: F1, f2: F2, f3: F3): T = macro InstanceMakerMacro.partialImpl3[T, F1, F2, F3]
    }
    def partial[T] = new partial[T]
  }
}
