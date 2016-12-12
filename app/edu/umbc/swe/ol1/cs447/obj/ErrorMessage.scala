package edu.umbc.swe.ol1.cs447.obj

import play.api.libs.json.Json

/**
  * A utility object for creating JSON error message objects.
  */
object ErrorMessage {
  /**
    * Returns a JSON object with a given error message.
    *
    * @param message the error message
    * @return a JSON object with the given error message
    */
  def apply(message: String) = Json.obj("message" -> message)

  /**
    * An error message for a request with an invalid body.
    */
  val invalidBody = apply("Invalid request body")

  /**
    * An error message for a request with invalid credentials.
    */
  val invalidCredentials = apply("Invalid credentials")

  /**
    * An error message for a request for a resource which does not exist.
    */
  val notFound = apply("Not Found")

  /**
    * An error message for a request with a new password which is not strong enough.
    */
  val passwordNotStrongEnough = apply("Password does not meet minimum strength criteria")
}
