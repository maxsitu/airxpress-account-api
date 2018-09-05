package controllers

import java.time.{Instant, ZoneOffset}

import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsError, Json, Reads}
import play.api.mvc._
import be.objectify.deadbolt.scala.{ActionBuilders, DeadboltActions}
import constant.SessionKeys
import dao.UserDao
import model._
import reactivemongo.core.errors.ReactiveMongoException
import security.SessionUtil
import validator.UserRegisterValidator
import views.html.accessOk

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserController @Inject()(implicit ec: ExecutionContext,
                               cc: ControllerComponents,
                               deadbolt: DeadboltActions,
                               actionBuilder: ActionBuilders,
                               userDao: UserDao,
                               sessionUtil: SessionUtil)
    extends AbstractController(cc) {

  import User._

  val log = play.api.Logger(getClass)

  def createUser = Action.async(validateJson[RegisterUser]) {
    request: Request[RegisterUser] ⇒
      val regUser: RegisterUser = request.body
      userDao
        .findUserByUsername(regUser.username)
        .flatMap {
          case None ⇒
            val currTime = Instant.now().atOffset(ZoneOffset.UTC)
            val user = for {
              roles ← UserRegisterValidator
                .validateUserRoles(regUser.userRoles)
                .right
              username ← UserRegisterValidator
                .validateUsername(regUser.username)
                .right
              dob ← UserRegisterValidator
                .validateDateOfBirth(regUser.dateOfBirth)
                .right
              email ← UserRegisterValidator.validateEmail(regUser.email).right
              psswd ← UserRegisterValidator
                .validatePassword(regUser.password)
                .right
            } yield
              User(
                regUser.username,
                regUser.firstName,
                regUser.lastName,
                regUser.dateOfBirth,
                regUser.userRoles,
                regUser.userPermissions,
                "",
                currTime,
                currTime
              )

            user.fold(
              // user validation failed in last step
              s ⇒ Future.successful(BadRequest(s)),
              // valid user
              { u ⇒
                import com.github.t3hnar.bcrypt._
                regUser.password
                  .bcryptSafe(10)
                  .fold(
                    // failed in generating password hash
                    e ⇒ Future.successful(BadRequest(e.getMessage)), {
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
    *
    * @return Action instance returns Future[Result]
    */
  def validateUser = Action.async(parse.json[LoginUser]) {
    request: Request[LoginUser] ⇒
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
    *
    * @return
    */
  def selfUserInfo = Action.async(parse.empty) { request: Request[Unit] ⇒
    if (sessionUtil.validateLoginTimestamp(request.session)) {
      log.debug(s"login timestamp valid")

      sessionUtil
        .extractUser(request.session)
        .map {
          case Right(user) ⇒
            log.debug(s"valid user extracted from session")
            Some(user)
          case Left(_) ⇒
            log.debug(s"invalid user extracted from session")
            None
        }
        .map {
          case Some(user) ⇒ Ok(Json.toJson(user))
          case None ⇒ Unauthorized("username in session not found")
        }
    } else {
      log.debug(s"login timestamp invalid")
      Future.successful(Unauthorized("not logged in or login age expired"))
    }
  }

  def userInfo(username: String) =
    deadbolt.Restrict(List(Array(AcctMgrUser.name), Array(AdminUser.name)))(
      parse.empty
    )(validateLoginTimestamp { authRequest ⇒
      userDao.findUserByUsername(username).map {
        case Some(user) =>
          Ok(Json.toJson(user))
        case None =>
          NotFound(s"username $username not found")
      }
    })

  def putUserRoles(username: String) =
    deadbolt.Restrict(List(Array(AcctMgrUser.name), Array(AdminUser.name)))(
      parse.json[List[String]]
    ) { request ⇒
      val roles =
        request.body.map(p ⇒ UserRole.parse(p)).foldLeft(Set[UserRole]()) {
          case (l: Set[UserRole], Some(role)) ⇒ l + role
          case (l: Set[UserRole], _) ⇒ l
        }

      def updateUser(currUser: User): Future[Result] = {
        val neoU = currUser.copy(userRoles = roles)
        userDao.updateUser(neoU).map(num ⇒ Ok(s"$num user affected"))
      }

      (
        for {
          opUserEither <- sessionUtil.extractUser(request.session)
          currUserOpt <- userDao.findUserByUsername(username)
        } yield (opUserEither, currUserOpt)
      ).flatMap {

        case (Right(opUser), Some(currUser))
            if currUser.roles
              .intersect(Array(AcctMgrUser.name, AdminUser.name))
              .isEmpty =>
          updateUser(currUser)

        case (Right(opUser), Some(currUser))
            if opUser.roles.contains(AdminUser.name) &&
              !currUser.roles.contains(AdminUser.name) =>
          log.debug(s"opUser roles: ${opUser.roles.mkString(",")}")
          updateUser(currUser)

        case (Right(opUser), Some(currUser)) =>
          log.debug(s"opUser roles: ${opUser.roles.mkString(",")}")
          log.debug(s"currUser roles: ${currUser.roles.mkString(",")}")
          Future.successful(
            Unauthorized(s"Unable to modify user $username, lack of permission")
          )

        case (_, None) =>
          Future.successful(BadRequest("User not exists"))

        case _ =>
          Future.successful(
            Unauthorized(s"Unable to modify user $username, lack of permission")
          )
      }
    }

  def patchUserRoles(username: String) =
    deadbolt.Restrict(List(Array(AcctMgrUser.name), Array(AdminUser.name)))(
      parse.json[Set[String]]
    ) { request ⇒
      val roleStrs = request.body
      if (roleStrs.isEmpty)
        Future.successful(
          BadRequest("request body should be a list of role string")
        )
      else {
        val roles: Set[UserRole] =
          roleStrs.map(p ⇒ UserRole.parse(p)).foldLeft(Set[UserRole]()) {
            case (l: Set[UserRole], Some(role)) ⇒ l + role
            case (l: Set[UserRole], _) ⇒ l
          }

        userDao
          .findUserByUsername(username)
          .flatMap {
            case Some(user)
                if user.userRoles
                  .intersect(Set(AcctMgrUser, AdminUser))
                  .isEmpty ⇒
              val neoU = user.copy(userRoles = user.userRoles.union(roles))
              userDao.updateUser(neoU).map(num ⇒ Ok(s"$num user affected"))

            case Some(user) =>
              Future.successful(
                Unauthorized(
                  s"Cannot change role of ${AdminUser.name} or ${AcctMgrUser.name}"
                )
              )

            case None ⇒ Future.successful(BadRequest("User not exists"))
          }
      }
    }

  def putUserPermissions(username: String) =
    deadbolt.Restrict(List(Array(AcctMgrUser.name), Array(AdminUser.name)))(
      parse.json[Set[String]]
    ) { request ⇒
      val permissions = request.body.map(p ⇒ UserPermission(p))

      val result: Future[Result] = userDao
        .findUserByUsername(username)
        .flatMap {
          case Some(user)
              if user.roles
                .intersect(Array(AcctMgrUser.name, AdminUser.name))
                .isEmpty ⇒ {
            val neoU = user.copy(userPermissions = permissions)
            userDao.updateUser(neoU).map(num ⇒ Ok(s"$num user affected"))
          }

          case Some(user) =>
            Future.successful(
              Unauthorized(
                s"Cannot change permissions of ${AdminUser.name} or ${AcctMgrUser.name}"
              )
            )

          case None ⇒ Future.successful(BadRequest("User not exists"))
        }

      result
    }

  def patchUserPermissions(username: String) =
    deadbolt.Restrict(List(Array(AcctMgrUser.name), Array(AdminUser.name)))(
      parse.json[Set[String]]
    ) { request ⇒
      val patchingPerms: Set[UserPermission] =
        request.body.map(p ⇒ UserPermission(p))

      def updateUser(currUser: User): Future[Result] = {
        val neoU = currUser.copy(
          userPermissions = currUser.userPermissions.union(patchingPerms)
        )
        userDao.updateUser(neoU).map(num ⇒ Ok(s"$num user affected"))
      }

      (for {
        opUserEither <- sessionUtil.extractUser(request.session)
        currUserOpt <- userDao.findUserByUsername(username)
      } yield (opUserEither, currUserOpt)).flatMap {

        case (Right(opUser), Some(currUser))
            if currUser.roles
              .intersect(Array(AcctMgrUser.name, AdminUser.name))
              .isEmpty =>
          updateUser(currUser)

        case (Right(opUser), Some(currUser))
            if opUser.roles.contains(AdminUser.name) && !currUser.roles
              .contains(AdminUser.name) =>
          updateUser(currUser)
        case (_, None) =>
          Future.successful(BadRequest("User not exists"))

        case _ =>
          Future.successful(
            Unauthorized(s"Unable to modify $username, lack of permission")
          )
      }
    }

  def restrictOne =
    deadbolt.Restrict(List(Array(AcctMgrUser.name)))()(validateLoginTimestamp {
      authRequest =>
        Future {
          Ok(accessOk())
        }
    })

  private def validateJson[A: Reads] =
    parse.json.validate(
      _.validate[A].asEither.left.map(e => BadRequest(JsError.toJson(e)))
    )

  private def validateLoginTimestamp(
    block: Request[Any] ⇒ Future[Result]
  ): Request[Any] ⇒ Future[Result] = { request ⇒
    if (sessionUtil.validateLoginTimestamp(request.session))
      block(request)
    else
      Future.successful(Unauthorized("logged-in too long ago, relogin"))
  }

}
