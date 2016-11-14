package models

import slick.driver.SQLiteDriver.api._
import slick.lifted.Tag

class Posts(tag: Tag) extends Table[Post](tag, "POSTS") {
  def postId = column[String]("POST_ID", O.PrimaryKey, O.Length(Posts.postIdLengthBase64, varying = false))
  def authorId = column[String]("AUTHOR_ID", O.Length(Accounts.userIdLength, varying = false))
  def account = foreignKey("AUTHOR_ID_FK", authorId, Accounts)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)
  def title = column[String]("TITLE")
  def body = column[String]("BODY")
  def category = column[String]("CATEGORY")
  def * = (postId, authorId, title, body, category) <> (Post.tupled, Post.unapply)
}

object Posts extends TableQuery[Posts](new Posts(_)) {
  val postIdLengthBase64 = 12
  val postIdLengthBytes = postIdLengthBase64 / 4 * 3

  {
    assert(postIdLengthBase64 % 4 == 0)
  }

  def withId(postId: String) = filter(_.postId === postId).result
}
