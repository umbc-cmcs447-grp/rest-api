package controllers

import javax.inject.{Inject, Singleton}

import edu.umbc.swe.ol1.cs447.core.{AccountManager, TokenManager}
import edu.umbc.swe.ol1.cs447.obj.ErrorMessage
import edu.umbc.swe.ol1.cs447.util.FutureFromOption
import models.{User, Users}
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.mvc.{Action, Controller, Result}

import scala.concurrent.Future

@Singleton
class UserController @Inject()(accountManager: AccountManager,
                               tokenManager: TokenManager,
                               dbConfProvider: DatabaseConfigProvider) extends Controller {
  private def db = dbConfProvider.get.db
  private implicit val errorMessageWrites = Json.writes[ErrorMessage]
  private implicit val userWrites = Json.writes[User]

  def createAccount = TODO

  def getInfo(id: String) = Action.async(parse.empty) {
    implicit request => {
      for (user <- getUser(id)) yield Ok(Json.toJson(user))
    } recover {
      case _ => NotFound(Json.toJson(ErrorMessage("User not found")))
    }
  }

  private def getUser(id: String): Future[User] = {
    for {
      userOpt <- db.run(Users.withId(id))
      user <- FutureFromOption(userOpt)
    } yield user
  }

  def login(id: String) = Action.async(parse.json) {
    implicit request =>
      (request.body \ "password").validate[String] match {
        case s: JsSuccess[String] => loginCheckCredentials(id, s.get)
        case e: JsError => Future.successful(BadRequest(Json.toJson(ErrorMessage("Missing password field"))))
      }
  }

  private def loginCheckCredentials(id: String, password: String): Future[Result] = {
    for {
      token <- accountManager.authenticate(id, password)
    } yield Ok(Json.obj("authToken" -> token))
  } recover {
    case _ => Unauthorized(Json.toJson(ErrorMessage.invalidCredentials))
  }

  def logout(id: String) = Action.async(parse.empty) {
    implicit request => {
      for {
        _ <- tokenManager.authenticateRequestForUser(request.headers, id)
        _ <- tokenManager.revokeToken(id)
      } yield NoContent
    } recover {
      case _ => Forbidden(Json.toJson(ErrorMessage.invalidCredentials))
    }
  }

  def updatePassword(id: String) = TODO
}
