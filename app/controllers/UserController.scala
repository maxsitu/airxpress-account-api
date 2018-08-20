package controllers

import java.time.{Instant, ZoneOffset}

import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsError, JsValue, Json, Reads}
import play.api.mvc._
import be.objectify.deadbolt.scala.{ActionBuilders, DeadboltActions}
import constant.SessionKeys
import dao.UserDao
import model._
import model.UserRole._
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
  sessionUtil: SessionUtil) extends AbstractController(cc) with ModelJsonImplicits {

  val log = play.api.Logger(getClass)

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
            regUser.username, regUser.firstName, regUser.lastName, regUser.dateOfBirth, regUser.userRoles, regUser.userPermissions, "", currTime, currTime
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
              {
                hashedPassword ⇒
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
  def validateUser = Action.async(parse.json[LoginUser]) { request: Request[LoginUser] ⇒

    val loginUser: LoginUser = request.body
    import com.github.t3hnar.bcrypt._
    userDao.findUserByUsername(loginUser.username).map {
      case Some(u: User) if (loginUser.password.isBcrypted(u.password)) ⇒
        Ok(u.userRoles.mkString(","))
          .withSession(
            SessionKeys.USERNAME → u.username,
            SessionKeys.LOGIN_TIMESTAMP → Instant.now().toEpochMilli.toString
          )
      case _ ⇒
        Unauthorized("invalid username/password")
    }
  }

  /**
    * Retrieve user information.
    * @return
    */
  def userInfo = Action.async(parse.empty) { request: Request[Unit] ⇒

    if (sessionUtil.validateLoginTimestamp(request.session)) {
      log.debug(s"login timestamp valid")

      sessionUtil.extractUser(request.session).map {
        case Right(user) ⇒
          log.debug(s"valid user extracted from session")
          Some(user)
        case Left(_)     ⇒
          log.debug(s"invalid user extracted from session")
          None
      }.map {
        case Some(user) ⇒ Ok(Json.toJson(user))
        case None       ⇒ Unauthorized("username in session not found")
      }
    } else {
      log.debug(s"login timestamp invalid")
      Future.successful(Unauthorized("not logged in or login age expired"))
    }
  }

  def user(username: String) = deadbolt.Restrict(List(Array(AcctMgr.name)))()(
    validateLoginTimestamp { authRequest ⇒
      userDao.findUserByUsername(username).map {
        case Some(user) =>
          Ok(Json.toJson(user))
        case None =>
          NotFound(s"username $username not found")
      }
    }
  )

  def putUserRoles(username: String) =
    deadbolt.Restrict(List(Array(AcctMgr.name)))(parse.json[List[String]]) { request ⇒

      val roleStrs = request.body
      if (roleStrs.isEmpty)
        Future.successful(BadRequest("role list is empty"))
      else {
        val badRole = roleStrs.find(role ⇒ UserRole.parse(role).isLeft)
        if (badRole.isDefined)
          Future.successful(BadRequest(s"bad role: ${badRole.get}"))
        else {
          val roles = roleStrs.map(p ⇒ UserRoleValue(p))

          userDao.findUserByUsername(username)
            .flatMap {
              case Some(user) ⇒
                val neoU = user.copy(userRoles = roles)
                userDao.updateUser(neoU).map(num ⇒ Ok(s"$num user affected"))

              case None ⇒ Future.successful(BadRequest("User not exists"))
            }
        }
      }
    }

  def patchUserRules(username: String) =
    deadbolt.Restrict(List(Array(AcctMgr.name)))(parse.json[List[String]]) { request ⇒

      val roleStrs = request.body
      if (roleStrs.isEmpty || roleStrs.exists(role ⇒ UserRole.parse(role).isLeft))
        Future.successful(BadRequest("request body should be a list of role string"))
      else {
        val roles = roleStrs.map(p ⇒ UserRoleValue(p))

        userDao.findUserByUsername(username)
          .flatMap {
            case Some(user) ⇒
              val neoU = user.copy(userRoles = user.roles.union(roles))
              userDao.updateUser(neoU).map(num ⇒ Ok(s"$num user affected"))

            case None ⇒ Future.successful(BadRequest("User not exists"))
          }
      }
    }

  def putUserPermissions(username: String) =
    deadbolt.Restrict(List(Array(AcctMgr.name)))(parse.json[List[String]]) { request ⇒
      val permissions = request.body.map(p ⇒ UserPermission(p))

      val result: Future[Result] = userDao.findUserByUsername(username)
        .flatMap {
          case Some(user) ⇒ {
            val neoU = user.copy(userPermissions = permissions)
            userDao.updateUser(neoU).map(num ⇒ Ok(s"$num user affected"))
          }
          case None ⇒ Future.successful(BadRequest("User not exists"))
        }

      result
    }

  def patchUserPermissions(username: String) =
    deadbolt.Restrict(List(Array(AcctMgr.name)))(parse.json[List[String]]) { request ⇒
      val permissions = request.body.map(p ⇒ UserPermission(p))

      val result: Future[Result] = userDao.findUserByUsername(username)
        .flatMap {
          case Some(user) ⇒ {
            val neoU = user.copy(userPermissions = user.userPermissions.union(permissions))
            userDao.updateUser(neoU).map(num ⇒ Ok(s"$num user affected"))
          }
          case None ⇒ Future.successful(BadRequest("User not exists"))
        }

      result
    }

  def restrictOne = deadbolt.Restrict(List(Array(AcctMgr.name)))()(
    validateLoginTimestamp { authRequest =>
      Future {
        Ok(accessOk())
      }
    }
  )


  private def validateJson[A : Reads] = parse.json.validate(
    _.validate[A].asEither.left.map(e => BadRequest(JsError.toJson(e)))
  )

  private def validateLoginTimestamp(block: Request[Any] ⇒ Future[Result]): Request[Any] ⇒ Future[Result] = { request ⇒
    if (sessionUtil.validateLoginTimestamp(request.session))
      block(request)
    else
      Future.successful(Unauthorized("logged-in too long ago, relogin"))
  }
}
