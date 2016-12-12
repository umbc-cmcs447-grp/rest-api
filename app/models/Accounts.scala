package models

import slick.driver.SQLiteDriver.api._
import slick.lifted.Tag

/**
  * Table for storing user account information.
  *
  * @param tag the [[Tag]]
  */
class Accounts(tag: Tag) extends Table[Account](tag, "ACCOUNTS") {
  def id = column[String]("ID", O.PrimaryKey, O.Length(Accounts.userIdLength, varying = false))
  def passwordHash = column[String]("PW_HASH_WITH_SALT")
  def admin = column[Boolean]("ADMIN", O.Default(false))
  def * = (id, passwordHash, admin) <> (Account.tupled, Account.unapply)
}

/**
  * Table query for user account information.
  */
object Accounts extends TableQuery[Accounts](new Accounts(_)) {
  val userIdLength = 7

  def getWithId(id: String) = filter(_.id === id).result.headOption
}
