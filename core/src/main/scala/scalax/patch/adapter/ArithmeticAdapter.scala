package scalax.patch.adapter

import java.{util => ju, time => jt, sql => jsql}

sealed trait ArithmeticAdapter[T] {
  type Delta

  def diff(left: T, right: T): Delta
  def reduce(base: T, delta: Delta): T
  def increase(base: T, delta: Delta): T

  class ArithmeticOps(lhs: T) {
    def -(rhs: T): Delta    = diff(lhs, rhs)
    def :-(delta: Delta): T = reduce(lhs, delta)
    def :+(delta: Delta): T = increase(lhs, delta)
  }

  implicit def mkLinearOps(lhs: T): ArithmeticOps = new ArithmeticOps(lhs)
}

object ArithmeticAdapter {

  type Aux[T, D] = ArithmeticAdapter[T] { type Delta = D }

  implicit def forNumeric[T](implicit num: Numeric[T]): Aux[T, T] = new ArithmeticAdapter[T] {
    type Delta = T

    def diff(left: T, right: T): Delta     = num.minus(left, right)
    def reduce(base: T, delta: Delta): T   = num.minus(base, delta)
    def increase(base: T, delta: Delta): T = num.plus(base, delta)
  }

  implicit def forTemporal[T <: jt.temporal.Temporal]: Aux[T, jt.Duration] = new ArithmeticAdapter[T] {
    type Delta = jt.Duration

    def diff(left: T, right: T): Delta     = jt.Duration.between(right, left)
    def reduce(base: T, delta: Delta): T   = base.minus(delta).asInstanceOf[T]
    def increase(base: T, delta: Delta): T = base.plus(delta).asInstanceOf[T]
  }

  implicit def forJsqlDate: Aux[jsql.Date, jt.Period] = new ArithmeticAdapter[jsql.Date] {
    type Delta = jt.Period

    def diff(left: jsql.Date, right: jsql.Date): Delta     = jt.Period.between(right.toLocalDate, left.toLocalDate)
    def reduce(base: jsql.Date, delta: Delta): jsql.Date   = jsql.Date.valueOf(base.toLocalDate.minus(delta))
    def increase(base: jsql.Date, delta: Delta): jsql.Date = jsql.Date.valueOf(base.toLocalDate.plus(delta))
  }

  implicit def forJsqlTime: Aux[jsql.Time, jt.Duration] = new ArithmeticAdapter[jsql.Time] {
    type Delta = jt.Duration

    def diff(left: jsql.Time, right: jsql.Time): Delta     = jt.Duration.between(right.toLocalTime, left.toLocalTime)
    def reduce(base: jsql.Time, delta: Delta): jsql.Time   = jsql.Time.valueOf(base.toLocalTime.minus(delta))
    def increase(base: jsql.Time, delta: Delta): jsql.Time = jsql.Time.valueOf(base.toLocalTime.plus(delta))
  }

  implicit def forJsqlTimestamp: Aux[jsql.Timestamp, jt.Duration] = new ArithmeticAdapter[jsql.Timestamp] {
    type Delta = jt.Duration

    def diff(left: jsql.Timestamp, right: jsql.Timestamp): Delta     = jt.Duration.between(right.toInstant, left.toInstant)
    def reduce(base: jsql.Timestamp, delta: Delta): jsql.Timestamp   = jsql.Timestamp.from(base.toInstant.minus(delta))
    def increase(base: jsql.Timestamp, delta: Delta): jsql.Timestamp = jsql.Timestamp.from(base.toInstant.plus(delta))
  }

  implicit def forJUDate: Aux[ju.Date, jt.Duration] = new ArithmeticAdapter[ju.Date] {
    type Delta = jt.Duration

    def diff(left: ju.Date, right: ju.Date): Delta     = jt.Duration.between(right.toInstant, left.toInstant)
    def reduce(base: ju.Date, delta: Delta): ju.Date   = ju.Date.from(base.toInstant.minus(delta))
    def increase(base: ju.Date, delta: Delta): ju.Date = ju.Date.from(base.toInstant.plus(delta))
  }
}
