package edu.umbc.swe.ol1.cs447.core.posts

/**
  * An enumeration of post statuses.
  */
object PostStatus extends Enumeration {
  type PostStatus = Value
  val OPEN, CLOSED, IN_PROGRESS = Value
}
