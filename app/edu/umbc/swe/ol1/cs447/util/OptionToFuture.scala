package edu.umbc.swe.ol1.cs447.util

import scala.concurrent.Future
import scala.util.Try

object OptionToFuture {
  implicit final class Option2Future[T](private val option: Option[T]) extends AnyVal {
    def toFuture: Future[T] = Future.fromTry(Try(option.get))
  }
}
