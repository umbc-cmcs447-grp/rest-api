package edu.umbc.swe.ol1.cs447.core

/**
  * An authentication token.
  *
  * @param authToken the string value of the token
  * @param maxExpiry the Unix timestamp (in millis) after which the token is invalid
  */
case class Token(authToken: String, maxExpiry: Long)
