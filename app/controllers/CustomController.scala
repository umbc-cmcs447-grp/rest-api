package controllers

import play.api.mvc.Controller
import play.api.mvc.Result

/**
  * Custom [[Controller]] trait to add implicit methods.
  */
trait CustomController extends Controller {
  implicit final class RichResult(private val result: Result) {
    /**
      * Adds a 'Location' header to a response.
      *
      * @param location the value of the Location header
      * @return a response with a Location header
      */
    def withLocation(location: String): Result = result.withHeaders("Location" -> location)
  }
}
