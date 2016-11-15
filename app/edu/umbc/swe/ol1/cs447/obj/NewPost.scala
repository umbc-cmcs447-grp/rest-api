package edu.umbc.swe.ol1.cs447.obj

import edu.umbc.swe.ol1.cs447.core.posts.Category.Category

case class NewPost(title: String, body: String, category: Category)
