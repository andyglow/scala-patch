package scalax

trait PatchMainPlus {  this: PatchMain =>

  def run(): Unit = {

    doPatch(
      LazyList("1", "2"),
      LazyList("12")
    )
  }
}