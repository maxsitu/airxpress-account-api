import javax.inject.{Inject, Provider, Singleton}
import play.api.{Configuration, Environment, OptionalSourceMapper, UsefulException}
import play.api.http.{DefaultHttpErrorHandler, HttpErrorHandler}
import play.api.mvc.RequestHeader
import play.api.mvc.Results._
import play.api.routing.Router

import scala.concurrent.Future

@Singleton
class ErrorHandler @Inject() (
  env: Environment,
  config: Configuration,
  sourceMapper: OptionalSourceMapper,
  router: Provider[Router]
) extends DefaultHttpErrorHandler(env, config, sourceMapper, router) {

  override def onProdServerError(request: RequestHeader, exception: UsefulException) = {
    Future.successful(
      InternalServerError("A server error occurred: " + exception.getMessage)
    )
  }

  override def onForbidden(request: RequestHeader, message: String) = {
    Future.successful(
      Forbidden("You're not allowed to access this resource.")
    )
  }

  override def onBadRequest(request: RequestHeader, message: String) = {
    Future.successful(
      BadRequest(s"$message")
    )
  }
}