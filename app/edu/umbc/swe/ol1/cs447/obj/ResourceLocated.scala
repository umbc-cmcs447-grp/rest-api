package edu.umbc.swe.ol1.cs447.obj

import play.api.libs.json.Json

/**
  * A utility object for creating response messages with a location.
  */
object ResourceLocated {
  /**
    * Returns a JSON object containing a given message and location.
    *
    * @param message the message
    * @param location the location (URI)
    * @return a JSON object containing the message and location
    */
  def apply(message: String, location: String) = Json.obj("message" -> message, "location" -> location)
}
