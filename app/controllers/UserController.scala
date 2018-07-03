package controllers

import java.time.{Instant, ZoneOffset}

import be.objectify.deadbolt.scala.{ActionBuilders, DeadboltActions}
import constant.SessionKeys
import dao.UserDao
import javax.inject.{Inject, Singleton}
import model.UserRole._
import model._
import play.api.libs.json.{JsError, JsValue, Json, Reads}
import play.api.mvc._
import reactivemongo.core.errors.ReactiveMongoException
import security.SessionUtil
import validator.UserRegisterValidator
import views.html.accessOk

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserController @Inject()(
  implicit ec: ExecutionContext,
  cc: ControllerComponents,
  deadbolt: DeadboltActions,
  actionBuilder: ActionBuilders,
  userDao: UserDao,
  sessionUtil: SessionUtil) extends AbstractController(cc) {

  val logger = play.api.Logger(getClass)

  implicit val userPermissionReads = UserPermission.UserPermissionReads
  implicit val userRoleValueReads = UserRole.UserRoleValueReads
  implicit val registerUserReads = RegisterUser.RegisterUserReads

  implicit val userPermissionWrites = UserPermission.UserPermissionWrites
  implicit val userRoleValueWrites = UserRole.UserRoleValueWrites
  implicit val userWrites = User.UserWrites

  def validateJson[A : Reads] = parse.json.validate(
    _.validate[A].asEither.left.map(e => BadRequest(JsError.toJson(e)))
  )

  def createUser = Action.async(validateJson[RegisterUser]) { request: Request[RegisterUser] ⇒
    val regUser: RegisterUser = request.body
    userDao.findUserByUsername(regUser.username).flatMap {
      case None ⇒
        val currTime = Instant.now().atOffset(ZoneOffset.UTC)
        val user = for {
          roles     ← UserRegisterValidator.validateUserRoles(regUser.userRoles).right
          username  ← UserRegisterValidator.validateUsername(regUser.username).right
          dob       ← UserRegisterValidator.validateDateOfBirth(regUser.dateOfBirth).right
          email     ← UserRegisterValidator.validateEmail(regUser.email).right
          psswd     ← UserRegisterValidator.validatePassword(regUser.password).right
        } yield
          User(
            regUser.username, regUser.firstName, regUser.lastName, regUser.dateOfBirth, regUser.userRoles,
            regUser.userPermissions, "tempPsswd", currTime, currTime
          )

        user.fold(
          // user validation failed in last step
          s ⇒ Future.successful(BadRequest(s)),
          // valid user
          { u ⇒
            import com.github.t3hnar.bcrypt._
            regUser.password.bcryptSafe(10).fold(
              // failed in generating password hash
              e ⇒ Future.successful(BadRequest(e.getMessage)),
              { hashedPassword ⇒
                val newUser = u.copy(password = hashedPassword)
                userDao.createUser(newUser).map(_ ⇒ Ok("success"))
              }
            )
          }
        )

      case Some(u: User) ⇒
        Future.successful(Conflict("username exists already"))

    } recover {
      case e: ReactiveMongoException ⇒
        InternalServerError(e.getMessage)
    }
  }

  /**
    * Validate username/password. Update session with username and login time.
    * It takes username/password as request body
    * @return Action instance returns Future[Result]
    */
  def validateUser = Action.async(parse.json) { request: Request[JsValue] ⇒
    val userPasswordOpt = (
      (request.body \ "username").asOpt[String],
      (request.body \ "password").asOpt[String])

    userPasswordOpt match {
      case (Some(username), Some(password)) ⇒
        import com.github.t3hnar.bcrypt._
        userDao.findUserByUsername(username).map {

          case Some(u:User) if (password.isBcrypted(u.password)) ⇒
            Ok(u.userRoles.mkString(","))
              .withSession(
                SessionKeys.USERNAME → u.username,
                SessionKeys.LOGIN_TIMESTAMP → Instant.now().toEpochMilli.toString
              )
          case _ ⇒
            Unauthorized("invalid username/password")
        }
      case _ ⇒
        Future.successful(
          BadRequest("username/password missing")
        )
    }
  }

  def userInfo = Action.async(parse.empty) { request: Request[Unit] ⇒
    if (sessionUtil.validateLoginTimestamp(request.session))
      sessionUtil.extractUser(request.session).map {
        case Right(user) ⇒ Some(user)
        case Left(_)     ⇒ None
      }.map {
        case Some(user) ⇒ Ok(Json.toJson(user))
        case None       ⇒ Unauthorized("username in session not found")
      }
    else
      Future.successful(Unauthorized("not logged in or login age expired"))
  }

  def user(username: String) = deadbolt.Restrict(List(Array(AcctMgr.name)))()(
    validateLoginTimestamp { authRequest ⇒
      userDao.findUserByUsername(username).map(userOpt ⇒
        if (userOpt.isDefined) Ok(Json.toJson(userOpt.get))
        else NotFound(s"username ${username} not found")
      )
    }
  )

  def restrictOne = deadbolt.Restrict(List(Array(AcctMgr.name)))()(
    validateLoginTimestamp { authRequest =>
      Future {
        Ok(accessOk())
      }
    }
  )

  def validateLoginTimestamp(block: Request[Any] ⇒ Future[Result]): Request[Any] ⇒ Future[Result] = { request ⇒
    if (sessionUtil.validateLoginTimestamp(request.session))
      block(request)
    else
      Future.successful(Unauthorized("logged-in too long ago, relogin"))
  }
}
