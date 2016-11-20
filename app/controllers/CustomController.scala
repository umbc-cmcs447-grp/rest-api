package controllers

import play.api.mvc.Controller
import play.api.mvc.Result

trait CustomController extends Controller {
  implicit final class RichResult(private val result: Result) {
    def withLocation(location: String): Result = result.withHeaders("Location" -> location)
  }
}
