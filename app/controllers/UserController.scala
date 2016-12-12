package controllers

import javax.inject.{Inject, Singleton}

import com.nulabinc.zxcvbn.Zxcvbn
import edu.umbc.swe.ol1.cs447.core.{AccountManager, TokenAuthException, TokenManager}
import edu.umbc.swe.ol1.cs447.obj.{ErrorMessage, NewUser, PasswordUpdate, ResourceLocated}
import models.{Account, Accounts, User, Users}
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.mvc.{Action, Request, Result}
import slick.driver.JdbcProfile

import scala.concurrent.Future

/**
  * Controller for endpoints starting with `/users`.
  *
  * @param accountManager the [[AccountManager]]
  * @param tokenManager   the [[TokenManager]]
  * @param dbConfProvider the database configuration provider
  */
@Singleton
class UserController @Inject()(accountManager: AccountManager,
                               tokenManager: TokenManager,
                               dbConfProvider: DatabaseConfigProvider) extends CustomController {
  private implicit val writesUser = Json.writes[User]
  private implicit val readsNewUser = Json.reads[NewUser]
  private implicit val readsPasswordUpdate = Json.reads[PasswordUpdate]

  private val authTokenField = "authToken"
  private val dbConf = dbConfProvider.get[JdbcProfile]
  private val db = dbConf.db
  private val zxcvbn = new Zxcvbn

  import dbConf.driver.api._

  /**
    * Creates a new user account.
    *
    * @return a [[Result]] containing the location of the new account
    */
  def createUser = Action.async(parse.json)(implicit request => {
    request.body.validate[NewUser] match {
      case json: JsSuccess[NewUser] =>
        val newUser = json.get
        newUserCheckFields(newUser.copy(id = newUser.id.toUpperCase))
      case _: JsError => Future.successful(BadRequest(ErrorMessage.invalidBody))
    }
  })

  private def newUserCheckFields[_: Request](user: NewUser): Future[Result] = {
    if (!accountManager.isValidAccountId(user.id)) {
      Future.successful(UnprocessableEntity(ErrorMessage("Invalid ID")))
    } else if (user.firstName.isEmpty || user.lastName.isEmpty || user.password.isEmpty) {
      Future.successful(UnprocessableEntity(ErrorMessage("Fields cannot be empty")))
    } else if (!checkPasswordStrength(user.password)) {
      Future.successful(UnprocessableEntity(ErrorMessage.passwordNotStrongEnough))
    } else newUserCheckExists(user)
  }

  private def checkPasswordStrength(password: String): Boolean = zxcvbn.measure(password).getScore > 2

  private def newUserCheckExists[_: Request](newUser: NewUser): Future[Result] = {
    for {
      exists <- db.run(Accounts.filter(_.id === newUser.id).exists.result)
      if !exists
      res <- createNewUser(newUser)
    } yield res
  } recover {
    case _: NoSuchElementException => Forbidden(ErrorMessage("User already exists with ID: " + newUser.id))
  }

  private def createNewUser(newUser: NewUser)(implicit request: Request[_]): Future[Result] = {
    val account = Account(newUser.id, accountManager.newHashedPassword(newUser.password))
    val user = User(newUser.id, newUser.firstName, newUser.lastName)
    for {
      _ <- db.run(Accounts += account)
      _ <- db.run(Users += user)
      location = userLocation(newUser.id)
    } yield Created(ResourceLocated("Account created", location)).withLocation(location)
  }

  private def userLocation(id: String)(implicit request: Request[_]) = request.host + "/users/" + id

  /**
    * Returns the information about a user.
    *
    * @param id the user's ID
    * @return the information about the user
    */
  def getInfo(id: String) = Action.async(parse.empty)(implicit request => {
    for (user <- db.run(Users.withId(id.toUpperCase))) yield Ok(Json.toJson(user))
  } recover {
    case _: NoSuchElementException => NotFound(ErrorMessage.notFound)
  })

  /**
    * Logs in as a user.
    *
    * @param id the user's ID
    * @return a response containing an authentication token
    */
  def login(id: String) = Action.async(parse.json) { implicit request =>
    (request.body \ "password").validate[String] match {
      case json: JsSuccess[String] => loginCheckCredentials(id.toUpperCase, json.get)
      case _: JsError => Future.successful(BadRequest(ErrorMessage("Missing password field")))
    }
  }

  private def loginCheckCredentials(id: String, password: String): Future[Result] = {
    for {
      token <- accountManager.authenticate(id, password)
    } yield Ok(Json.obj(authTokenField -> token))
  } recover {
    case _: NoSuchElementException => Unauthorized(ErrorMessage.invalidCredentials)
  }

  /**
    * Check if a user's authentication token is valid.
    *
    * @param id the user's ID
    * @return an empty response if the token is valid
    */
  def validateAuth(id: String) = Action.async(parse.empty)(implicit request => {
    for {
      _ <- tokenManager.authenticateRequestForUser(request.headers, id.toUpperCase)
    } yield NoContent
  } recover {
    case _: TokenAuthException => Forbidden(ErrorMessage.invalidCredentials)
  })

  /**
    * Logs a user out.
    *
    * @param id the user's ID
    * @return and empty response if logout was successful
    */
  def logout(id: String) = Action.async(parse.empty)(implicit request => {
    val idUpper = id.toUpperCase
    for {
      _ <- tokenManager.authenticateRequestForUser(request.headers, idUpper)
      _ <- tokenManager.revokeToken(idUpper)
    } yield NoContent
  } recover {
    case _: TokenAuthException => Forbidden(ErrorMessage.invalidCredentials)
  })

  /**
    * Change a user's password.
    *
    * @param id the user's ID
    * @return an authentication token if the action was successful
    */
  def updatePassword(id: String) = Action.async(parse.json) { implicit request =>
    request.body.validate[PasswordUpdate] match {
      case json: JsSuccess[PasswordUpdate] =>
        val passwordUpdate = json.get
        if (!checkPasswordStrength(passwordUpdate.newPassword)) {
          Future.successful(UnprocessableEntity(ErrorMessage.passwordNotStrongEnough))
        } else changePassword(id.toUpperCase, passwordUpdate)
      case e: JsError => Future.successful(BadRequest(ErrorMessage.invalidBody))
    }
  }

  private def changePassword(id: String, passwordUpdate: PasswordUpdate): Future[Result] = {
    for {
      token <- accountManager.authenticate(id, passwordUpdate.oldPassword)
      newAccount = Account(id, accountManager.newHashedPassword(passwordUpdate.newPassword))
      _ <- db.run(Accounts.filter(_.id === id).update(newAccount))
    } yield Ok(Json.obj("message" -> "Password changed", authTokenField -> token))
  } recover {
    case _: NoSuchElementException => Unauthorized(ErrorMessage.invalidCredentials)
  }
}
