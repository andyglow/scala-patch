package scalax.gpl.patch

trait ScalaVersionSpecificPatchSpecs { this: PatchSpecs =>

  doPatch(
    LazyList("1", "2"),
    LazyList("12")
  )
}
