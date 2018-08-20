package security

import java.time.Instant
import java.time.temporal.ChronoUnit

import constant.SessionKeys
import dao.UserDao
import javax.inject.{Inject, Singleton}
import model.User
import play.api.mvc.Session
import play.api.{ConfigLoader, Configuration}

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SessionUtil @Inject() (implicit ec: ExecutionContext, config: Configuration, userDao: UserDao) {

  val log = play.api.Logger(getClass)

  implicit val configLoader: ConfigLoader[Duration] = ConfigLoader.durationLoader

  val maxLoginAge = config.get[Duration]("user.login.maxAge")

  def extractUser(session: Session): Future[Either[String, User]] = {

    val result = session.get(SessionKeys.USERNAME) match {
      case None ⇒ Future.successful(Left("username not found in session"))
      case Some(username) ⇒
        userDao.findUserByUsername(username)
          .map(userOpt ⇒
            if (userOpt.isDefined) Right(userOpt.get)
            else Left(s"username ${username} not found in system")
          )
    }

    result
  }

  def validateLoginTimestamp(session: Session): Boolean = {
    log.debug(s"session login timestamp: ${session.get(SessionKeys.LOGIN_TIMESTAMP)}")

    session.get(SessionKeys.LOGIN_TIMESTAMP)
      .map(ts ⇒
        Instant.ofEpochMilli(ts.toLong))
      .exists(
        _.isAfter(
          Instant.now()
            .minus(maxLoginAge.toMinutes, ChronoUnit.MINUTES)
        )
      )
  }
}