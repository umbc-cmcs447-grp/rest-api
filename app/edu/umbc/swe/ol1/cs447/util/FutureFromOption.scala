package edu.umbc.swe.ol1.cs447.util

import scala.concurrent.Future
import scala.util.Try

object FutureFromOption {
  def apply[T](opt: Option[T]): Future[T] = Future.fromTry(Try(opt.get))
}
