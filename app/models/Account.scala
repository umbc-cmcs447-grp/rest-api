package models

/**
  * Representation for a user account.
  *
  * @param id           the user's ID
  * @param passwordHash the user's password hash
  * @param admin        whether or not the user is an admin
  */
case class Account(id: String, passwordHash: String, admin: Boolean = false)
