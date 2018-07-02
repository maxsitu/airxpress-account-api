package security

import be.objectify.deadbolt.scala.models.Subject
import be.objectify.deadbolt.scala.{AuthenticatedRequest, DeadboltHandler, DynamicResourceHandler}
import dao.UserDao
import javax.inject.{Inject, Singleton}
import play.api.mvc.{Request, Result, Results}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserRoleDeadboltHandler @Inject() (
                                          implicit  ec: ExecutionContext,
                                          userDao: UserDao,
                                          sessionUtil: SessionUtil
                                        ) extends DeadboltHandler {

  override def beforeAuthCheck[A](request: Request[A]): Future[Option[Result]] = Future(None)

  override def getSubject[A](request: AuthenticatedRequest[A]): Future[Option[Subject]] = {
    if (sessionUtil.validateLoginTimestamp(request.session))
      sessionUtil.extractUser(request.session).map {
        case Right(value) ⇒ Some(value)
        case Left(_) ⇒ None
      }
    else Future.successful(None)
  }

  override def onAuthFailure[A](request: AuthenticatedRequest[A]): Future[Result] = {
    Future { Results.Forbidden("access denied")}
  }

  override def getDynamicResourceHandler[A](request: Request[A]): Future[Option[DynamicResourceHandler]] = ???
}
