package security

import be.objectify.deadbolt.scala.models.Subject
import be.objectify.deadbolt.scala.{AuthenticatedRequest, DeadboltHandler, DynamicResourceHandler}
import dao.UserDao
import play.api.mvc.{Request, Result, Results}

import scala.concurrent.Future

class UserRoleDeadboltHandler () extends DeadboltHandler {
  import scala.concurrent.ExecutionContext.Implicits.global

  override def beforeAuthCheck[A](request: Request[A]): Future[Option[Result]] = Future(None)

  override def getSubject[A](request: AuthenticatedRequest[A]): Future[Option[Subject]] = {
    request.session.get("username") match {
      case None ⇒ Future(None)
      case Some(userId) ⇒ UserDao.findUserByUsername(userId)
    }
  }

  override def onAuthFailure[A](request: AuthenticatedRequest[A]): Future[Result] = {
    Future { Results.Forbidden("access denied")}
  }

  override def getDynamicResourceHandler[A](request: Request[A]): Future[Option[DynamicResourceHandler]] = ???
}
