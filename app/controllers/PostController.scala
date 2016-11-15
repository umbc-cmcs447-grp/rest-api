package controllers

import javax.inject.{Inject, Singleton}

import edu.umbc.swe.ol1.cs447.core.posts.{Category, PostStatus}
import edu.umbc.swe.ol1.cs447.core.{IdManager, TokenManager}
import edu.umbc.swe.ol1.cs447.obj.{ErrorMessage, NewPost, PostUpdate, ResourceLocated}
import models.{Accounts, Post, Posts}
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json._
import play.api.mvc.{Action, Controller, Request, Result}
import slick.driver.JdbcProfile

import scala.concurrent.Future

@Singleton
class PostController @Inject()(tokenManager: TokenManager,
                               idManager: IdManager,
                               dbConfProvider: DatabaseConfigProvider) extends Controller {
  private implicit val readsCategory = Reads.enumNameReads(Category)
  private implicit val readsStatus = Reads.enumNameReads(PostStatus)
  private implicit val readsNewPost = Json.reads[NewPost]
  private implicit val readsPostUpdate = Json.reads[PostUpdate]
  private implicit val writesPost = Json.writes[Post]

  private val dbConf = dbConfProvider.get[JdbcProfile]
  private val db = dbConf.db

  import dbConf.driver.api._

  def createPost = Action.async(parse.json) { implicit request =>
    request.body.validate[NewPost] match {
      case json: JsSuccess[NewPost] => createNewPost(json.get)
      case e: JsError => Future.successful(BadRequest(ErrorMessage.invalidBody))
    }
  }

  private def createNewPost(newPost: NewPost)(implicit request: Request[_]): Future[Result] = {
    for {
      authorId <- tokenManager.authenticateRequest(request.headers)
      postId <- idManager.newId
      timestamp = System.currentTimeMillis
      post = Post(
        postId = postId,
        authorId = authorId,
        title = newPost.title,
        body = newPost.body,
        category = newPost.category.toString,
        status = PostStatus.OPEN.toString,
        created = timestamp,
        lastModified = timestamp)
      _ <- db.run(Posts += post)
      location = postLocation(postId)
    } yield Created(ResourceLocated("Post created", location)).withHeaders("Location" -> location)
  } recover {
    case _ => Forbidden(ErrorMessage.invalidCredentials)
  }

  private def postLocation(postId: String)(implicit request: Request[_]): String = request.host + "/posts/" + postId

  def getPost(id: String) = Action.async(parse.empty)(implicit request => {
    for {
      post <- db.run(Posts.withId(id))
    } yield Ok(Json.toJson(post))
  } recover {
    case _ => NotFound(ErrorMessage.notFound)
  })

  def updatePost(id: String) = Action.async(parse.json) { implicit request =>
    request.body.validate[PostUpdate] match {
      case json: JsSuccess[PostUpdate] =>
        val update = json.get
        if (update == PostUpdate.emptyUpdate) {
          Future.successful(UnprocessableEntity(ErrorMessage("Post update cannot be empty")))
        } else updatePostCheckExists(id, json.get)
      case e: JsError => Future.successful(BadRequest(ErrorMessage.invalidBody))
    }
  }

  private def updatePostCheckExists[_: Request](id: String, update: PostUpdate): Future[Result] = {
    for {
      post <- db.run(Posts.withId(id))
      res <- updatePostCheckAuth(post, update)
    } yield res
  } recover {
    case _ => NotFound(ErrorMessage.notFound)
  }

  private def updatePostCheckAuth(post: Post, update: PostUpdate)(implicit request: Request[_]): Future[Result] = {
    for {
      userId <- tokenManager.authenticateRequest(request.headers)
      if userId == post.authorId
      res <- doUpdatePost(post, update)
    } yield res
  } recover {
    case _ => Forbidden(ErrorMessage.invalidCredentials)
  }

  private def doUpdatePost[_: Request](post: Post, update: PostUpdate): Future[Result] = {
    val updatedPost = post.copy(
      title = update.title.getOrElse(post.title),
      body = update.body.getOrElse(post.body),
      category = update.category.map(_.toString).getOrElse(post.category),
      status = update.status.map(_.toString).getOrElse(post.status),
      lastModified = System.currentTimeMillis
    )
    for {
      _ <- db.run(Posts.update(updatedPost))
      location = postLocation(post.postId)
    } yield Ok(ResourceLocated("Post updated", location)).withHeaders("Location" -> location)
  }

  def deletePost(id: String) = Action.async(parse.empty)(implicit request => {
    for {
      post <- db.run(Posts.withId(id))
      res <- doDeletePost(post)
    } yield res
  } recover {
    case _ => NotFound(ErrorMessage.notFound)
  })

  private def doDeletePost(post: Post)(implicit request: Request[_]): Future[Result] = {
    for {
      userId <- tokenManager.authenticateRequest(request.headers)
      account <- db.run(Accounts.getWithId(userId))
      if userId == post.authorId || (account.isDefined && account.get.admin)
      _ <- db.run(Posts.filter(_.postId === post.postId).delete)
    } yield NoContent
  } recover {
    case _ => Forbidden(ErrorMessage.invalidCredentials)
  }
}
