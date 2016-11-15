package models

import slick.driver.SQLiteDriver.api._
import slick.lifted.Tag

class Users(tag: Tag) extends Table[User](tag, "USERS") {
  def id = column[String]("ID", O.Length(Accounts.userIdLength, varying = false))
  def account = foreignKey("ID_FK", id, Accounts)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)
  def firstName = column[String]("FIRST_NAME")
  def lastName = column[String]("LAST_NAME")
  def * = (id, firstName, lastName) <> (User.tupled, User.unapply)
}

object Users extends TableQuery[Users](new Users(_)) {
  def withId(id: String) = filter(_.id === id).result.head
}
