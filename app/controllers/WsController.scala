package controllers

import actors.UserParentActor
import akka.NotUsed
import akka.actor._
import akka.pattern.ask
import akka.stream.scaladsl.Flow
import akka.util.Timeout
import javax.inject.{Inject, Named, Singleton}
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents, RequestHeader, WebSocket}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class WsController @Inject() (cc: ControllerComponents,
                              @Named("userParentActor") userParentActor: ActorRef)
                              (implicit ec: ExecutionContext)
  extends AbstractController(cc) {

  val logger = play.api.Logger(getClass)

  def ws: WebSocket = WebSocket.acceptOrResult[String, String] {
    case rh =>
      wsFutureFlow(rh).map { flow =>
        Right(flow)
      }.recover {
        case e: Exception =>
          logger.error("Cannot create websocket", e)
          val jsError = Json.obj("error" -> "Cannot create websocket")
          val result = InternalServerError(jsError)
          Left(result)
      }

    case rejected =>
      logger.error(s"Request ${rejected} failed same origin check")
      Future.successful {
        Left(Forbidden("forbidden"))
      }
  }

  /**
    * Creates a Future containing a Flow of JsValue in and out.
    */
  private def wsFutureFlow(request: RequestHeader): Future[Flow[String, String, NotUsed]] = {
    // Use guice assisted injection to instantiate and configure the child actor.
    implicit val timeout = Timeout(1.second) // the first run in dev can take a while :-(
    val future: Future[Any] = userParentActor ? UserParentActor.Create(request.id.toString)
    val futureFlow: Future[Flow[String, String, NotUsed]] = future.mapTo[Flow[String, String, NotUsed]]
    futureFlow
  }
}
