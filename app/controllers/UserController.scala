package controllers

import java.util.regex.Pattern
import javax.inject.{Inject, Singleton}

import edu.umbc.swe.ol1.cs447.core.{AccountManager, TokenManager}
import edu.umbc.swe.ol1.cs447.obj.{ErrorMessage, NewUser, PasswordUpdate, ResourceLocated}
import models.{Account, Accounts, User, Users}
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.mvc.{Action, Request, Result}
import slick.driver.JdbcProfile

import scala.concurrent.Future

@Singleton
class UserController @Inject()(accountManager: AccountManager,
                               tokenManager: TokenManager,
                               dbConfProvider: DatabaseConfigProvider) extends CustomController {
  private implicit val writesUser = Json.writes[User]
  private implicit val readsNewUser = Json.reads[NewUser]
  private implicit val readsPasswordUpdate = Json.reads[PasswordUpdate]

  private val authTokenField = "authToken"
  private val idPattern = Pattern.compile("[a-zA-Z]{2}\\d{5}")
  private val dbConf = dbConfProvider.get[JdbcProfile]
  private val db = dbConf.db

  import dbConf.driver.api._

  def createUser = Action.async(parse.json)(implicit request => {
    request.body.validate[NewUser] match {
      case json: JsSuccess[NewUser] =>
        val newUser = json.get
        newUserCheckFields(newUser.copy(id = newUser.id.toUpperCase))
      case e: JsError => Future.successful(BadRequest(ErrorMessage.invalidBody))
    }
  })

  private def newUserCheckFields[_: Request](user: NewUser): Future[Result] = {
    if (!idPattern.matcher(user.id).matches()) {
      Future.successful(UnprocessableEntity(ErrorMessage("Invalid ID")))
    } else if (user.firstName.isEmpty || user.lastName.isEmpty || user.password.isEmpty) {
      Future.successful(UnprocessableEntity(ErrorMessage("Fields cannot be empty")))
    } else if (!checkPasswordStrength(user.password)) {
      Future.successful(UnprocessableEntity(ErrorMessage.passwordNotStrongEnough))
    } else newUserCheckExists(user)
  }

  private def checkPasswordStrength(password: String): Boolean = {
    // TODO: Check password strength
    true
  }

  private def newUserCheckExists[_: Request](newUser: NewUser): Future[Result] = {
    for {
      user <- getUser(newUser.id)
    } yield Forbidden(ErrorMessage("User already exists with ID: " + newUser.id))
  } recoverWith {
    case _ => createNewUser(newUser)
  }

  private def createNewUser(newUser: NewUser)(implicit request: Request[_]): Future[Result] = {
    val account = Account(newUser.id, accountManager.newHashedPassword(newUser.password))
    val user = User(newUser.id, newUser.firstName, newUser.lastName)
    for {
      _ <- db.run(Accounts += account)
      _ <- db.run(Users += user)
      location = request.host + "/users/" + newUser.id
    } yield Created(ResourceLocated("Account created", location)).withLocation(location)
  }

  def getInfo(id: String) = Action.async(parse.empty)(implicit request => {
    for (user <- getUser(id.toUpperCase)) yield Ok(Json.toJson(user))
  } recover {
    case e: NoSuchElementException => NotFound(ErrorMessage.notFound)
  })

  private def getUser(id: String): Future[User] = db.run(Users.withId(id))

  def login(id: String) = Action.async(parse.json) { implicit request =>
    (request.body \ "password").validate[String] match {
      case s: JsSuccess[String] => loginCheckCredentials(id.toUpperCase, s.get)
      case e: JsError => Future.successful(BadRequest(ErrorMessage("Missing password field")))
    }
  }

  private def loginCheckCredentials(id: String, password: String): Future[Result] = {
    for {
      token <- accountManager.authenticate(id, password)
    } yield Ok(Json.obj(authTokenField -> token))
  } recover {
    case e: NoSuchElementException => Unauthorized(ErrorMessage.invalidCredentials)
  }

  def logout(id: String) = Action.async(parse.empty)(implicit request => {
    val idUpper = id.toUpperCase
    for {
      _ <- tokenManager.authenticateRequestForUser(request.headers, idUpper)
      _ <- tokenManager.revokeToken(idUpper)
    } yield NoContent
  } recover {
    case e: NoSuchElementException => Forbidden(ErrorMessage.invalidCredentials)
  })

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
      _ <- db.run(Accounts.update(newAccount))
    } yield Ok(Json.obj("message" -> "Password changed", authTokenField -> token))
  } recover {
    case e: NoSuchElementException => Unauthorized(ErrorMessage.invalidCredentials)
  }
}
