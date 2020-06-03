package scalax.patch.macros

import scala.reflect.macros.blackbox

private[macros] trait HasLog { this: HasContext =>

  val debugEnabled = true

  val dbg: String => Unit = if (debugEnabled) c.info(c.enclosingPosition, _, force = true) else _ => ()

  val info: String => Unit = c.info(c.enclosingPosition, _, force = true)

  val warn: String => Unit = c.warning(c.enclosingPosition, _)

  val err: String => Nothing = c.abort(c.enclosingPosition, _)
}
