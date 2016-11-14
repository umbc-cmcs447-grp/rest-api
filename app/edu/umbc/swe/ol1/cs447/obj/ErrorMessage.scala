package edu.umbc.swe.ol1.cs447.obj

import play.api.libs.json.Json

object ErrorMessage {
  def apply(message: String) = Json.obj("message" -> message)

  val invalidBody = apply("Invalid request body")
  val invalidCredentials = apply("Invalid credentials")
  val passwordNotStrongEnough = apply("Password does not meet minimum strength criteria")
}
