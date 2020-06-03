# Scala Patch
The library provides the AST for structured Patches over Scala standard types as well as type classes that know how to compute patches for different types.
Supported types:
- basic types like `string`, `boolean`, `numeric`
- temporal types (`java.time`, `java.util`, `java.sql`)
- collections
  - `unordered` (Seq, Iterable, ...)
  - `ordered` (LinerSeq, List, LazyList, ...)
  - `indexed` (Array, Vector, ...)
  - `keyed` (Maps)
- sum types (Option, Either)
   
## Example
```scala
import scalax.patch._

// values
val left = List(1, 2, 3)
val right = List(1, 3, 4, 5)

// make a patch
val patch = Patch.make(left, right)

// apply a patch
patch(left) == right

// apply inverted patch
patch.inverted(right) == left

// patch is structured
// toString
println(patch)
patch> UpdateOrdered(Diff(Upgrade(IncreaseValue(1),IncreaseValue(1)),Insert(4)))

// patch rendered 
PatchVisitor stringify patch
patch> upgrade
patch> - increase 1
patch> - increase 1
patch> insert
patch> - 4

// case classes
case class CC(
  name: String,
  age: Int,
  props: Map[String, String])

object CC {
  implicit val ccPM: PatchMaker[CC] = DerivePatchMaker.derive[CC]
}

val left = CC("shelly", 23, Map("prop1" -> "v1", "prop2" -> "v2"))
val right = CC("cristine", 37, Map("prop1" -> "vv1", "prop2" -> "vv2"))

val patch = Patch.make(left, right)
println(patch)
patch> $CC$Patch(UpdateValue(shelly,cristine),IncreaseValue(14),UpdateKeyed(Map(prop1 -> UpdateValue(v1,vv1), prop2 -> UpdateValue(v2,vv2))))

PatchVisitor stringify patch
patch> field 'name' {
patch>   update: from shelly
patch>         : to   cristine
patch> }
patch> field 'age' {
patch>   increase 14
patch> }
patch> field 'props' {
patch>   key 'prop1' {
patch>     update: from v1
patch>           : to   vv1
patch>   }
patch>   key 'prop2' {
patch>     update: from v2
patch>           : to   vv2
patch>   }
patch> } 
```   