package edu.umbc.swe.ol1.cs447.core

import javax.inject.{Inject, Singleton}

import akka.agent.Agent
import com.google.common.cache.{CacheBuilder, CacheLoader, LoadingCache}
import edu.umbc.swe.ol1.cs447.util.FutureFromOption
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc.Headers

import scala.concurrent.Future
import scala.concurrent.duration._


@Singleton
class TokenManager @Inject() () {
  private val maxExpiryMillis = DAYS.toMillis(1)
  private val authHeader = "X-NetBuz-Auth"

  private val cache: LoadingCache[String, Agent[Option[Token]]] =
    CacheBuilder.newBuilder()
      .expireAfterAccess(2, HOURS)
      .build(new CacheLoader[String, Agent[Option[Token]]] {
        override def load(key: String): Agent[Option[Token]] = Agent(None)
      })

  def authenticateRequestForUser(headers: Headers, user: String): Future[Unit] = {
    authenticateRequest(headers).filter(_ == user).map(_ => Unit)
  }

  def authenticateRequest(headers: Headers): Future[String] = {
    for {
      authStr <- FutureFromOption(headers.get(authHeader))
      (id, token) = authStr.split(":", 2) match {case Array(s1, s2) => (s1.toUpperCase, s2)}
      if checkToken(id, token)
    } yield id
  }

  def revokeToken(id: String): Future[Unit] = cache.get(id).alter(None).map(_ => Unit)

  def setToken(id: String, tokenString: String): Future[Option[Token]] = {
    cache.get(id).alter(_ => Some(Token(tokenString, System.currentTimeMillis + maxExpiryMillis)))
  }

  def checkToken(id: String, tokenString: String): Boolean = {
    val option = cache.get(id)()
    option.isDefined && System.currentTimeMillis < option.get.maxExpiry
  }
}
