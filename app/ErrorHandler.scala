import javax.inject.{Inject, Provider, Singleton}
import play.api.http.DefaultHttpErrorHandler
import play.api.mvc.RequestHeader
import play.api.mvc.Results._
import play.api.routing.Router
import play.api.{Configuration, Environment, OptionalSourceMapper, UsefulException}

import scala.concurrent.Future

@Singleton
class ErrorHandler @Inject() (
  env: Environment,
  config: Configuration,
  sourceMapper: OptionalSourceMapper,
  router: Provider[Router]
) extends DefaultHttpErrorHandler(env, config, sourceMapper, router) {
  import play.api.mvc.Result

  override def onProdServerError(request: RequestHeader, exception: UsefulException) = {
    Future.successful(
      InternalServerError("A server error occurred: " + exception.getMessage)
    )
  }

  override def onForbidden(request: RequestHeader, message: String) = {
    Future.successful(
      Forbidden(s"You're not allowed to access this resource. $message")
    )
  }

  override def onBadRequest(request: RequestHeader, message: String) = {
    Future.successful(
      BadRequest(s"$message")
    )
  }

  override def onNotFound(request: RequestHeader, message: String): Future[Result] = {
    Future.successful(NotFound(s"method: ${request.method}, uri: ${request.uri}"))
  }
}