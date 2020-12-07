package scalax

trait MakeMainPlus { this: MakeMain =>
  import MakeMainRunner._

  override def runExtra(): Unit = {
    implicit val defCC1 = Default(CC1(0, "foo"))

    println(make.from[CC1]('id -> 100L))
  }
}
