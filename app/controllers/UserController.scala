package controllers

import java.time.{Instant, ZoneOffset}

import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsError, Json, Reads}
import play.api.mvc._
import be.objectify.deadbolt.scala.{ActionBuilders, DeadboltActions}
import constant.SessionKeys
import dao.UserDao
import model._
import model.RichUser._
import model.UserRole._
import reactivemongo.core.errors.ReactiveMongoException
import security.SessionUtil
import validator.UserRegisterValidator
import views.html.accessOk
import be.objectify.deadbolt.scala.AuthenticatedRequest

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserController @Inject()(implicit ec: ExecutionContext,
                               cc: ControllerComponents,
                               deadbolt: DeadboltActions,
                               actionBuilder: ActionBuilders,
                               userDao: UserDao,
                               sessionUtil: SessionUtil)
    extends InjectedController {
  import akka.util.ByteString
  import play.api.http.Writeable

  val PASSWORD_MASK = "******"

  val log = play.api.Logger(getClass)

  def postRegisterUser = Action.async(validateJson[RegisterUser]) { request ⇒
    val regUser: RegisterUser = request.body
    userDao
      .findRichUserByUsername(regUser.username)
      .flatMap {
        case Some(u: RichUser) ⇒
          Future.successful(Conflict("username exists already"))

        case None ⇒
          import com.github.t3hnar.bcrypt._
          val currTime = Instant.now().atOffset(ZoneOffset.UTC)
          val userEitherValid: Either[String, RichUser] = for {
            username ← UserRegisterValidator.validateUsername(regUser.username).right
            firstname ← UserRegisterValidator.validateFirstname(regUser.firstName).right
            lastname ← UserRegisterValidator.validateLastname(regUser.lastName).right
            dob ← UserRegisterValidator.validateDateOfBirth(regUser.dateOfBirth).right
            email ← UserRegisterValidator.validateEmail(regUser.email).right
            psswd ← UserRegisterValidator.validatePassword(regUser.password).right
            hashedPsswd ← psswd.bcryptSafe(10).toEither.left.map(e ⇒ e.getStackTrace.mkString(System.lineSeparator))
          } yield RichUser(username, firstname, lastname, dob, Set.empty, hashedPsswd, currTime, currTime)


          Future.successful(userEitherValid.fold(errMsg ⇒ BadRequest(errMsg), u ⇒ Ok(Json.toJson(u))))

      } recover {
      case e: ReactiveMongoException ⇒
        InternalServerError(e.getMessage)
    }
  }

  /**
    * Validate username/password. Update session with username and login time.
    * It takes username/password as request body
    * Returns a User object in json with some fields
    * trimmed.
    *
    * @return Action instance returns Future[Result]
    */
  def postLoginUser = deadbolt.SubjectNotPresent()(parse.json[LoginUser]) { request ⇒
    val loginUser: LoginUser = request.body
    import com.github.t3hnar.bcrypt._
    userDao.findRichUserByUsername(loginUser.username).map {
      case Some(richUser: RichUser) if loginUser.password.isBcrypted(richUser.password) ⇒
        Ok(
            Json.toJson
            (
              genOutGoingUser(richUser)
            )
          )
          .withSession(
            SessionKeys.USERNAME        → richUser.username,
            SessionKeys.LOGIN_TIMESTAMP → Instant.now().toEpochMilli.toString
          )
      case _ ⇒
        Unauthorized("invalid username/password")
    }
  }

  /**
    * Retrieve user information.
    *
    * @return
    */
  def getSelfUser =
    deadbolt.SubjectPresent()(parse.empty)(validateLoginTimestamp { authRequest ⇒
      val richUser = authRequest.subject.map(_.asInstanceOf[RichUser]).get
      Future.successful(Ok(Json.toJson(genOutGoingUser(richUser))))
    })

  def getManagingUser(username: String) =
    deadbolt.Pattern("user.read")(parse.empty)(validateLoginTimestamp { authRequest ⇒
      userDao.findUserByUsername(username).map {
        case Some(user) =>
          Ok(Json.toJson(user))
        case None =>
          NotFound(s"username $username not found")
      }
    })

  def putSelfUser(username: String) =
    deadbolt.SubjectPresent()(parse.json[SelfUpdatUser])(validateLoginTimestamp { authRequest ⇒
      val selfUpdatUser = authRequest.body
      authRequest.subject.map(_.asInstanceOf[RichUser]) match {
        case Some(user) ⇒ Future.successful(Ok(Json.toJson(user)))
        case None       ⇒ Future.successful(Unauthorized("not logged in or login age expired"))
      }
    })

  def getUserRoles() = Action.async (parse.empty) {request ⇒
    userDao.findAllUserRoles().map(roles ⇒
      Ok(Json.toJson(roles)))
  }

  def postUserRole() = Action.async (parse.json[UserRole]) {request ⇒
    val newRole = request.body
    userDao.createRole(newRole).map(n ⇒
      Ok(n.toString))
  }

  def putUserRole() = Action.async (parse.json[UserRole]) {request ⇒
    val newRole = request.body
    userDao.updateUserRole(newRole).map(n ⇒
      Ok(n.toString))
  }

  private def validateJson[A: Reads] =
    parse.json.validate(
      _.validate[A].asEither.left.map(e => BadRequest(JsError.toJson(e)))
    )

  private def validateLoginTimestamp[T](
      block: AuthenticatedRequest[T] ⇒ Future[Result]
  ): AuthenticatedRequest[T] ⇒ Future[Result] = { request ⇒
    if (sessionUtil.validateLoginTimestamp(request.session))
      block(request)
    else
      Future.successful(Unauthorized("logged-in too long ago, relogin"))
  }

  private def genOutGoingUser(richUser: RichUser): User = User(richUser).copy(password = PASSWORD_MASK)
}