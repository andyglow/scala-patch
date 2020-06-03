package scalax.patch.adapter

sealed trait Sum2Adapter[F[_, _], L, R] {

  def wrapLeft(x: L): F[L, R]

  def wrapRight(x: R): F[L, R]

  def unwrapLeft(x: F[L, R]): L

  def unwrapRight(x: F[L, R]): R

  def exract(l: F[L, R], r: F[L, R]): Option[Either[(L, L), (R, R)]]
}

object Sum2Adapter {

  implicit def forEither[L, R]: Sum2Adapter[Either, L, R] = new Sum2Adapter[Either, L, R] {

    override def wrapLeft(x: L): Either[L, R] = Left(x)

    override def wrapRight(x: R): Either[L, R] = Right(x)

    override def unwrapLeft(x: Either[L, R]): L = x.swap.getOrElse(throw new IllegalStateException)

    override def unwrapRight(x: Either[L, R]): R = x.getOrElse(throw new IllegalStateException)

    override def exract(l: Either[L, R], r: Either[L, R]): Option[Either[(L, L), (R, R)]] = (l, r) match {
      case (Left(l1), Left(l2))   => Some(Left((l1, l2)))
      case (Right(r1), Right(r2)) => Some(Right((r1, r2)))
      case _                      => None
    }
  }
}