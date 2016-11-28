package controllers

import javax.inject.{Inject, Singleton}

import edu.umbc.swe.ol1.cs447.core.AccountManager
import edu.umbc.swe.ol1.cs447.core.posts.{Category, PostStatus}
import edu.umbc.swe.ol1.cs447.obj.ErrorMessage
import models.{Post, Posts}
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.Json
import play.api.mvc.{Action, Request, Result}
import slick.driver.JdbcProfile

import scala.concurrent.Future
import scala.util.Try

@Singleton
class SearchController @Inject() (dbConfProvider: DatabaseConfigProvider,
                                  accountManager: AccountManager) extends CustomController {
  private val dbConf = dbConfProvider.get[JdbcProfile]
  private val db = dbConf.db

  import dbConf.driver.api._

  private implicit val writesPost = Json.writes[Post]

  def searchPosts = Action.async(parse.empty)(implicit request => {
    val users = seqFromQueryString("users", accountManager.isValidAccountId, mapping = _.toUpperCase)
    val categories = seqFromQueryString("categories", s => Try(Category.withName(s)).isSuccess)
    val statuses = seqFromQueryString("statuses", s => Try(PostStatus.withName(s)).isSuccess)

    if (users.isLeft) {
      Future.successful(BadRequest(ErrorMessage("Invalid user ID: " + users.left.get)))
    } else if (categories.isLeft) {
      Future.successful(BadRequest(ErrorMessage("Invalid category: " + categories.left.get)))
    } else if (statuses.isLeft) {
      Future.successful(BadRequest(ErrorMessage("Invalid status: " + statuses.left.get)))
    } else searchWithConstraints(users.right.get, categories.right.get, statuses.right.get)
  })

  private def seqFromQueryString(key: String,
                                 isValid: String => Boolean,
                                 mapping: String => String = identity)
                                (implicit request: Request[_]): Either[String, Seq[String]] = {
    val seq = request.queryString.getOrElse(key, Nil)
      .flatMap(_.split(",").toStream.map(_.trim))
      .map(mapping)

    val invalidElement = seq.toStream.dropWhile(isValid).headOption

    if (invalidElement.isDefined) Left(invalidElement.get) else Right(seq)
  }

  private def searchWithConstraints(users: Seq[String],
                                    categories: Seq[String],
                                    statuses: Seq[String]): Future[Result] = {
    def matchSeq(seq: Seq[String]): Seq[Rep[String] => Rep[Boolean]] = {
      if (seq.isEmpty) List(_ => true)
      else seq.map(s => (r: Rep[String]) => r === s)
    }

    val query: Query[Posts, Posts#TableElementType, Seq] = {
      for {
        c <- matchSeq(categories)
        u <- matchSeq(users)
        s <- matchSeq(statuses)
      } yield Posts.filter(p => c(p.category) && u(p.authorId) && s(p.status))
    } reduce {_ union _}

    {
      for {
        seq <- db.run(query.result)
        if seq.nonEmpty
      } yield Ok(Json.toJson(seq))
    } recover {
      case _: NoSuchElementException => NotFound(ErrorMessage.notFound)
    }
  }
}
