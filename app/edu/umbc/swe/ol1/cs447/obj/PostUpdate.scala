package edu.umbc.swe.ol1.cs447.obj

import edu.umbc.swe.ol1.cs447.core.posts.Category.Category
import edu.umbc.swe.ol1.cs447.core.posts.PostStatus.PostStatus

case class PostUpdate(title: Option[String],
                      body: Option[String],
                      category: Option[Category],
                      status: Option[PostStatus])

object PostUpdate {
  val emptyUpdate = apply(None, None, None, None)
}
