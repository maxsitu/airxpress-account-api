package security

import be.objectify.deadbolt.scala.models.Subject
import be.objectify.deadbolt.scala.{AuthenticatedRequest, DeadboltHandler, DynamicResourceHandler}
import dao.UserDao
import javax.inject.{Inject, Singleton}
import play.api.mvc.{Request, Result, Results}
import play.api.Logger

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserRoleDeadboltHandler @Inject()(
    implicit ec: ExecutionContext,
    sessionUtil: SessionUtil,
    userDao: UserDao
) extends DeadboltHandler {

  val log = Logger(classOf[UserRoleDeadboltHandler])

  override def beforeAuthCheck[A](request: Request[A]): Future[Option[Result]] = Future(None)

  override def getSubject[A](request: AuthenticatedRequest[A]): Future[Option[Subject]] = {
    sessionUtil.extractUser(request.session).map {
      case Right(value) ⇒
        log.debug(s"User value extracted from session ${value.toString}")
        Some(value)
      case Left(_) ⇒
        log.debug(s"User value not found from session")
        None
    }
  }

  override def onAuthFailure[A](request: AuthenticatedRequest[A]): Future[Result] = {
    Future {
      Results.Forbidden("access denied")
    }
  }

  override def getPermissionsForRole(roleName: String): Future[List[String]] = for {
    userRoleOpt <- userDao.findUserRole(roleName)
    permissions ← userRoleOpt.fold(Future.successful(List.empty[String]))(result ⇒ Future.successful(result.permissions.toList))
  } yield (permissions)

  override def getDynamicResourceHandler[A](
      request: Request[A]): Future[Option[DynamicResourceHandler]] = Future { None }
}
