# Scala GPL
> GPL: Generic Programming Library

The library aims to provide several concepts missed in vanilla scala:
- (make) enhanced approach to create new instances of case classes
- (patch) ability to compare, create patch, apply patch for standard scala types

[![Build Status](https://cloud.drone.io/api/badges/andyglow/scala-patch/status.svg)](https://cloud.drone.io/andyglow/scala-patch)
![Maven Central](https://img.shields.io/maven-central/v/com.github.andyglow/scala-gpl_2.13?color=%234c1&label=maven)
![Codecov](https://img.shields.io/codecov/c/gh/andyglow/scala-patch)

Supported types:
- basic types like `string`, `boolean`, `numeric`
- temporal types (`java.time`, `java.util`, `java.sql`)
- collections
  - `unordered` (Seq, Iterable, ...)
  - `ordered` (LinerSeq, List, LazyList, ...)
  - `indexed` (Array, Vector, ...)
  - `keyed` (Maps)
- generic sum types (Option, Either)
- product types (case classes)
   
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
```

## Introduction
Often we need more specific explanation of why one value is not equal to another or, more specifically,
what is a delta of two values, or
what should be modified in value `A` (and how) so it become equal to value `B`.

When we may need it?
- Testing. More detailed difference reports.
- In network enabled applications where update remote state (patch size is in average less in size then an updated state copy) 
- In CQRS applications it might be helpful to disassemble a diff of 2 states into a sequence of events.

## Design

### Algebra
The Patch is a sum type of following definition
```
Patch[T] =
    UpdateValue                     (from: T, to: T)                                                |
    SetValue                        (to: T)                                                         |
    UnsetValue                      (from: T)                                                       |
    IncreaseValue                   (delta: ArithmeticAdapter[T]#Delta)                             |
    DecreaseValue                   (delta: ArithmeticAdapter[T]#Delta)                             |
    UpdateIndexed   [F[_], V]       (delta: Map[Int, Patch[V]], sizeDelta: Int) where T = F[V]      |
    UpdateKeyed     [F[_, _], K, V] (delta: Map[K, Patch[T]])                   where T = F[K, V]   |      
    UpdateUnordered [F[_], V]       (delta: UnorderedAdapter[F, T]#Diff)        where T = F[V] and 
        UnorderedAdapter.Diff[T] = Seq[UnorderedAdapter.Diff.Evt[T]] where
            UnorderedAdapter.Diff.Evt[T] =
                Add(items: Seq[T])    |
                Remove(items: Seq[T])                                                               |
    UpdateOrdered   [F[_], V]       (delta: OrderedAdapter[F, T]#Diff)          
        OrderedAdapter.Diff[T] = List[OrderedAdapter.Diff.Evt[T]] where
            OrderedAdapter.Diff.Evt[T] =
                Skip(n: Int)                    |
                Insert(items: List[T])          |
                Drop(items: List[T])            |
                Update(patches: List[Patch[T]])
  
```

## Derivation
Patch Maker derivation is provided for case classes.

```scala
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

## Text
By default string patch gives you a Constant patch (SetValue, UpdateValue, UnsetValue), 
which under some circumstances may look non optimal.

For more sophisticated string manipulation you can use `texts` module`
```scala
libraryDependencies += "com.github.andyglow" %% "scala-patch-texts" % $version
```

It is based on google's `patch-match-diff` library and give more detailed patches over strings.

All you need is
```scala
import scalax.patch.texts._
```

Example:
```scala
import scalax.patch.texts._

val patch = Patch.make("hello, dear friend!", "hello, my friend!")
patch> TextPatch(Step(3, 3, 12, 10, Equal("lo, "), Delete("dear"), Insert("my"), Equal(" fri"))) 
```
