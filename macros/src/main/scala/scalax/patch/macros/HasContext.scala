package scalax.patch.macros

import scala.reflect.macros.blackbox


private[macros] trait HasContext {

  val c: blackbox.Context

}
