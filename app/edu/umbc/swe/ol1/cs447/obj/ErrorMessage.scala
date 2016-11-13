package edu.umbc.swe.ol1.cs447.obj

case class ErrorMessage(message: String)

object ErrorMessage {
  val invalidCredentials = ErrorMessage("Invalid credentials")
}
