package edu.umbc.swe.ol1.cs447.obj

import edu.umbc.swe.ol1.cs447.core.posts.Category.Category
import edu.umbc.swe.ol1.cs447.core.posts.PostStatus.PostStatus

/**
  * An update to a post
  * @param title an [[Option]] containing the new title of the post, if it is changed
  * @param body an [[Option]] containing the new body of the post, if it is changed
  * @param category an [[Option]] containing the new category of the post, if it is changed
  * @param status an [[Option]] containing the new status of the post, if it is changed
  */
case class PostUpdate(title: Option[String],
                      body: Option[String],
                      category: Option[Category],
                      status: Option[PostStatus])

object PostUpdate {
  /**
    * A post update with no changes
    */
  val emptyUpdate = apply(None, None, None, None)
}
