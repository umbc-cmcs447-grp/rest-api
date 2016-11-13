package controllers

import javax.inject.{Inject, Singleton}

import edu.umbc.swe.ol1.cs447.{AccountManager, TokenManager}
import play.api.mvc.{Action, Controller}

import scala.concurrent.Future

@Singleton
class AccountController @Inject() (accountManager: AccountManager, tokenManager: TokenManager) extends Controller {
  def createAccount = TODO

  def login(id: String) = Action.async(parse.json) {
    implicit request =>
      Future.successful(Ok) // TODO: change
  }

  def logout(id: String) = TODO

  def updatePassword(id: String) = TODO
}
