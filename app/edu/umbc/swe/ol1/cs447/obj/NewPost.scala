package edu.umbc.swe.ol1.cs447.obj

import edu.umbc.swe.ol1.cs447.core.posts.Category.Category

/**
  * A new post.
  *
  * @param title the title of the post
  * @param body the body of the post
  * @param category the category of the post
  */
case class NewPost(title: String, body: String, category: Category)
