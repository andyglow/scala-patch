package scalax


trait MakeMain {

  def runExtra(): Unit = ()
}
object MakeMainRunner extends MakeMain with MakeMainPlus {

  case class CC1(id: Long, name: String)
  case class CC2(name: String, id: Long)

  case class Composition(
    name1: String,
    name2: String,
    name3: String,
    name4: String)

  case class Part1(name1: String, name2: String)
  case class Part2(name3: Int, name4: String)
  case class Part3(name3: String, name4: Int)

  def main(args: Array[String]): Unit = {
    implicit val defCC1 = Default(CC1(0, "foo"))

    println(make.from[CC1]("id" -> 100L, "name" -> "foo"))
    println(make.from[CC1](100L, "foo"))
    println(make.from[CC1]("name" -> "foo", "id" -> 0))
    println(make.from[CC1]("id" -> 100L))

//    println(make.from[CC1]('bar -> 100L, 'baz -> "xxx"))

    println(make[CC1](id = 100L))
//    println(make[CC1](id = 100L, bar = "qwe"))
    println(make[CC1](100L))

    println(make.from.partial[CC1](CC2("foox", 200)))
    println(make.from.partial[Composition](
      Part1("1", "2"),
      Part2(3, "4"),
      Part3("3", 4)))

    runExtra()
  }
}
