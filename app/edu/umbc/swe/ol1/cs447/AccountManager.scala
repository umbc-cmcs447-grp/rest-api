package edu.umbc.swe.ol1.cs447

import java.security.SecureRandom
import javax.inject.{Inject, Singleton}

import models.{Account, Accounts}
import org.apache.commons.codec.binary.Hex
import org.springframework.security.core.token.Sha512DigestUtils
import org.springframework.security.crypto.bcrypt.BCrypt
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits._

@Singleton
class AccountManager @Inject()(tokenManager: TokenManager, databaseConfigProvider: DatabaseConfigProvider) {
  private val tokenSize = 24
  private val rand: SecureRandom = new SecureRandom

  private val dummyAccount = {
    val dummyHash = BCrypt.hashpw(Sha512DigestUtils.shaHex("dummy"), BCrypt.gensalt())
    Account("dummy12", dummyHash)
  }

  private def authenticateUser(id: String, password: String) = {
    for {
      option <- Accounts.getWithId(id)
      account = option.getOrElse(dummyAccount)
    } yield BCrypt.checkpw(Sha512DigestUtils.shaHex(password), account.passwordHash) && option.nonEmpty
  }

  private def genToken(): String = {
    val bytes = Array.ofDim[Byte](tokenSize)
    rand.nextBytes(bytes)
    Hex.encodeHexString(bytes)
  }

  private def setTokenForUser(id: String, token: String) = tokenManager.setToken(id, token)

  def authenticate(id: String, password: String) = {
    for {
      allowed <- databaseConfigProvider.get.db.run(authenticateUser(id, password))
      if allowed
      token = {
          val t = genToken()
          setTokenForUser(id, t)
          t
      }
    } yield token
  }
}
