package actors

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.event.LoggingReceive
import akka.pattern.{ask, pipe}
import akka.stream.scaladsl.Flow
import akka.util.Timeout
import javax.inject.{Inject, Named}
import play.api.Configuration

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class UserParentActor @Inject()(configuration: Configuration,
                                @Named("activeMqActor") activeMqActor: ActorRef)
                               (implicit ec: ExecutionContext)
  extends Actor with ActorLogging {

  import Messages._
  import UserParentActor._

  implicit val timeout = Timeout(2.seconds)

  private val defaultMsgs = configuration.get[Seq[String]]("default.messages")

  override def receive: Receive = LoggingReceive {
    case Create(id) ⇒
      val name = s"userActor-$id"
      val future = (activeMqActor ? WatchStocks(defaultMsgs.toSet)).mapTo[Flow[String, String, _]]
      pipe(future) to sender()
  }
}

object UserParentActor {
  case class Create(id: String)
}