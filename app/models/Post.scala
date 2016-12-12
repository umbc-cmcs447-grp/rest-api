package models

/**
  * Representation for a post.
  *
  * @param postId       the ID of the post
  * @param authorId     the ID of the post's author
  * @param title        the title of the post
  * @param body         the body of the post
  * @param category     the category of the post
  * @param status       the status of the post
  * @param created      Unix millis timestamp of when the post was created
  * @param lastModified Unix millis timestamp of when the post was last modified
  */
case class Post(postId: String,
                authorId: String,
                title: String,
                body: String,
                category: String,
                status: String,
                created: Long,
                lastModified: Long)
