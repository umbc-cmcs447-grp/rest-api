package edu.umbc.swe.ol1.cs447.obj

import play.api.libs.json.Json

object ErrorMessage {
  def apply(message: String) = Json.obj("message" -> message)

  val invalidCredentials = apply("Invalid credentials")
}
