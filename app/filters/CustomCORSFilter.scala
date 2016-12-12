package filters

import javax.inject.{Inject, Singleton}

import akka.stream.Materializer
import play.api.http.HeaderNames
import play.api.mvc.{Filter, RequestHeader, Result}
import play.filters.cors.CORSFilter

import scala.concurrent.{ExecutionContext, Future}

/**
  * A custom CORS filter which allows 'null' origins.
  *
  * @param corsFilter the builtin CORS filter from Play2
  * @param mat        the materializer
  * @param exec       the execution context
  */
@Singleton
class CustomCORSFilter @Inject()(corsFilter: CORSFilter)
                                (implicit override val mat: Materializer,
                                 exec: ExecutionContext) extends Filter {

  override def apply(nextFilter: RequestHeader => Future[Result])
                    (requestHeader: RequestHeader): Future[Result] = {
    requestHeader.headers.get(HeaderNames.ORIGIN) match {
      case (Some("null")) =>
        val newHeaders = requestHeader.headers
          .remove(HeaderNames.ORIGIN)
          .add(HeaderNames.ORIGIN -> "http://file.url.local.null")
        val mappedOrigin = requestHeader.copy(headers = newHeaders)
        corsFilter(nextFilter)(mappedOrigin)
          .map { result =>
            result.withHeaders(HeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN -> "null")
          }
      case _ => corsFilter(nextFilter)(requestHeader)
    }
  }
}
