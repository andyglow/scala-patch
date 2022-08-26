package scalax.gpl.patch.macros

import scala.reflect.macros.blackbox

private[macros] trait UContext {

  val c: blackbox.Context

}
