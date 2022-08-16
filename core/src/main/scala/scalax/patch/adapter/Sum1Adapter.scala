package scalax.patch.adapter

sealed trait Sum1Adapter[F[_], T] {

  def wrap(x: T): F[T]

  def unwrap(x: F[T]): T

  def extract(l: F[T], r: F[T]): Option[(T, T)]
}

object Sum1Adapter {

  implicit def forOption[T]: Sum1Adapter[Option, T] = new Sum1Adapter[Option, T] {
    override def wrap(x: T): Option[T] = Some(x)
    override def unwrap(x: Option[T]): T = x.get
    override def extract(l: Option[T], r: Option[T]): Option[(T, T)] = for {l <- l; r <- r} yield (l, r)
  }
}

