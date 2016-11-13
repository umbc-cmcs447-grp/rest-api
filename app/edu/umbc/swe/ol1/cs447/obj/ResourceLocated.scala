package edu.umbc.swe.ol1.cs447.obj

import play.api.libs.json.Json

object ResourceLocated {
  def apply(message: String, location: String) = Json.obj("message" -> message, "location" -> location)
}
