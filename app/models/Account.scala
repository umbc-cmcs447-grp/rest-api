package models

case class Account(id: String, passwordHash: String, admin: Boolean = false)
