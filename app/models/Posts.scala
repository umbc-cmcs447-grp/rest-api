package models

import slick.driver.SQLiteDriver.api._
import slick.lifted.Tag

/**
  * Table for storing post information.
  *
  * @param tag the [[Tag]]
  */
class Posts(tag: Tag) extends Table[Post](tag, "POSTS") {
  def postId = column[String]("POST_ID", O.PrimaryKey, O.Length(Posts.postIdLengthBase64, varying = false))
  def authorId = column[String]("AUTHOR_ID", O.Length(Accounts.userIdLength, varying = false))
  def account = foreignKey("AUTHOR_ID_FK", authorId, Accounts)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)
  def title = column[String]("TITLE")
  def body = column[String]("BODY")
  def category = column[String]("CATEGORY")
  def status = column[String]("STATUS")
  def created = column[Long]("CREATED")
  def lastModified = column[Long]("LAST_MODIFIED")
  def * = (postId, authorId, title, body, category, status, created, lastModified) <> (Post.tupled, Post.unapply)
}

/**
  * Table query for post information.
  */
object Posts extends TableQuery[Posts](new Posts(_)) {
  val postIdLengthBase64 = 12
  val postIdLengthBytes = postIdLengthBase64 / 4 * 3

  {
    assert(postIdLengthBase64 % 4 == 0)
  }

  def withId(postId: String) = filter(_.postId === postId).result.head
}
