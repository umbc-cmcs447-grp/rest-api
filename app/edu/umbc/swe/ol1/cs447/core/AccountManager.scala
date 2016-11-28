package edu.umbc.swe.ol1.cs447.core

import java.security.SecureRandom
import java.util.Base64
import java.util.regex.Pattern
import javax.inject.{Inject, Singleton}

import models.{Account, Accounts}
import org.springframework.security.core.token.Sha512DigestUtils
import org.springframework.security.crypto.bcrypt.BCrypt
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits._

@Singleton
class AccountManager @Inject()(tokenManager: TokenManager, dbConfProvider: DatabaseConfigProvider) {
  private val tokenSize = 24
  private val rand: SecureRandom = new SecureRandom
  private val dummyAccount = Account("dummy12", newHashedPassword("dummy"))
  private val idPattern = Pattern.compile("[a-zA-Z]{2}\\d{5}")

  def isValidAccountId(id: String): Boolean = idPattern.matcher(id).matches

  def newHashedPassword(password: String): String = BCrypt.hashpw(Sha512DigestUtils.shaHex(password), BCrypt.gensalt())

  private def authenticateUser(id: String, password: String) = {
    for {
      option <- Accounts.getWithId(id)
      account = option.getOrElse(dummyAccount)
    } yield BCrypt.checkpw(Sha512DigestUtils.shaHex(password), account.passwordHash) && option.nonEmpty
  }

  private def genToken(): String = {
    val bytes = Array.ofDim[Byte](tokenSize)
    rand.nextBytes(bytes)
    Base64.getUrlEncoder.encodeToString(bytes)
  }

  private def setTokenForUser(id: String, token: String) = tokenManager.setToken(id, token)

  def authenticate(id: String, password: String) = {
    for {
      allowed <- dbConfProvider.get.db.run(authenticateUser(id, password))
      if allowed
      token = {
          val t = genToken()
          setTokenForUser(id, t)
          t
      }
    } yield token
  }
}
