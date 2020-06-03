package scalax

import scalax.patch.adapter.collections.OrderedCollectionAdapter.Diff

object DiffComputeMain {

  def main(args: Array[String]): Unit = {
    val l = List(1,2,3,4)
    val r = List(5,7,3,4,8,9)

    println(Diff.compute(l, r))
    println(Diff.compute(l, r).inverted)
  }
}
