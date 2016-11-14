package controllers

import javax.inject.{Inject, Singleton}

import edu.umbc.swe.ol1.cs447.core.{IdManager, TokenManager}
import play.api.mvc.Controller

@Singleton
class PostController @Inject()(tokenManager: TokenManager, idGenerator: IdManager) extends Controller {
  def createPost = TODO

  def getPost(id: String) = TODO

  def updatePost(id: String) = TODO

  def deletePost(id: String) = TODO
}
