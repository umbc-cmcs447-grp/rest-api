package edu.umbc.swe.ol1.cs447.core

import javax.inject.{Inject, Singleton}

import akka.agent.Agent
import com.google.common.cache.{CacheBuilder, CacheLoader, LoadingCache}
import edu.umbc.swe.ol1.cs447.util.OptionToFuture._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc.Headers

import scala.concurrent.Future
import scala.concurrent.duration._


/**
  * A singleton for managing authentication tokens.
  */
@Singleton
class TokenManager @Inject() () {
  private val maxExpiryMillis = DAYS.toMillis(2)
  private val authHeader = "X-NetBuz-Auth"

  private val cache: LoadingCache[String, Agent[Option[Token]]] =
    CacheBuilder.newBuilder()
      .expireAfterAccess(4, HOURS)
      .build(new CacheLoader[String, Agent[Option[Token]]] {
        override def load(key: String): Agent[Option[Token]] = Agent(None)
      })

  /**
    * Checks that a request has a valid authentication token for a given user.
    *
    * Returns a [[Future]] which will succeed (with [[Unit]]) if the request
    * is properly authenticated for the user, and will fail with a
    * [[TokenAuthException]] if the authentication is invalid.
    *
    * @param headers the headers of the request to be checked
    * @param user the user for whom the request should authenticate
    * @return a Future which will succeed if the request has valid authentication
    */
  def authenticateRequestForUser(headers: Headers, user: String): Future[Unit] = {
    for {
      id <- authenticateRequest(headers)
      if id == user
    } yield {}
  } recover {
    case _: NoSuchElementException => throw new TokenAuthException
  }

  /**
    * Checks that a request has a valid authentication token.
    *
    * Returns a [[Future]] which will succeed with the ID of the user
    * authenticating if the request is properly authenticated, and
    * will fail with a [[TokenAuthException]] if the authentication is invalid.
    *
    * @param headers the headers of the request to be checked
    * @return a Future which will succeed if the request has valid authentication
    */
  def authenticateRequest(headers: Headers): Future[String] = {
    for {
      authStr <- headers.get(authHeader).toFuture
      (id, token) = authStr.split(":", 2) match {case Array(s1, s2) => (s1.trim.toUpperCase, s2.trim)}
      if checkToken(id, token)
    } yield id
  } recover {
    case _ => throw new TokenAuthException
  }

  /**
    * Revokes the authentication token for a user, if one exists.
    *
    * @param id the ID of the user whose token will be revoked
    * @return a Future which will succeed once the token is revoked
    */
  def revokeToken(id: String): Future[Unit] = cache.get(id).alter(None).map(_ => Unit)

  /**
    * Sets an authentication token for a user.
    *
    * @param id the ID of the user for whom to set the token
    * @param tokenString the token to set
    * @return a Future which will succeed once the token is set
    */
  def setToken(id: String, tokenString: String): Future[Option[Token]] = {
    cache.get(id).alter(_ => Some(Token(tokenString, System.currentTimeMillis + maxExpiryMillis)))
  }

  private def checkToken(id: String, tokenString: String): Boolean = {
    val option = cache.get(id)()
    option.isDefined && System.currentTimeMillis < option.get.maxExpiry
  }
}
