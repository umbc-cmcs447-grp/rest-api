package edu.umbc.swe.ol1.cs447.core

case class Token(authToken: String, inactivityExpiry: Long, maxExpiry: Long)
