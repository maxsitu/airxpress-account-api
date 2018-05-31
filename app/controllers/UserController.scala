package controllers

import java.time.{Instant, ZoneOffset}

import be.objectify.deadbolt.scala.{ActionBuilders, DeadboltActions}
import constant.SessionKeys
import dao.UserDao
import javax.inject.{Inject, Singleton}
import model._
import play.api.libs.json.{JsError, JsValue, Json, Reads}
import play.api.mvc.{AbstractController, ControllerComponents, Request}
import reactivemongo.core.errors.ReactiveMongoException
import validator.UserRegisterValidator
import views.html.accessOk

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserController @Inject()(
  implicit ec: ExecutionContext,
  cc: ControllerComponents,
  deadbolt: DeadboltActions,
  actionBuilder: ActionBuilders,
  userDao: UserDao) extends AbstractController(cc) {

  implicit val userReads: Reads[RegisterUser] = Json.reads[RegisterUser]

  def validateJson[A : Reads] = parse.json.validate(
    _.validate[A].asEither.left.map(e => BadRequest(JsError.toJson(e)))
  )

  def createUser = Action.async(validateJson[RegisterUser]) { request: Request[RegisterUser] ⇒
    val regUser: RegisterUser = request.body

    userDao.findUserByUsername(regUser.username).flatMap {
      case None ⇒
        val currTime = Instant.now().atOffset(ZoneOffset.UTC)
        val user = for {
          roles ← UserRegisterValidator.validateUserRoles(regUser.userRoles).right
          username ← UserRegisterValidator.validateUsername(regUser.username).right
          dob ← UserRegisterValidator.validateDateOfBirth(regUser.dateOfBirth).right
          email ← UserRegisterValidator.validateEmail(regUser.email).right
          psswd ← UserRegisterValidator.validatePassword(regUser.password).right
        } yield

          User(regUser.username, regUser.firstName, regUser.lastName, regUser.dateOfBirth,
            regUser.userRoles.map(SecurityRole.parseRole(_).getOrElse(SecurityRole.NORMAL)), "psswd",
            currTime, currTime)

        user.fold(
          s ⇒ Future.successful(BadRequest(s)),
          { u ⇒
            import com.github.t3hnar.bcrypt._
            val hashedPasswordTry = regUser.password.bcryptSafe(10)

            hashedPasswordTry.fold(
              e ⇒ Future.successful(BadRequest(e.getMessage)),

              { hashedPassword ⇒
                val newUser = u.copy(password = hashedPassword)
                userDao.createUser(newUser).map(_ ⇒ Ok("success"))
              }
            )
          }
        )

      case Some(u: User) ⇒
        Future.successful(BadRequest("username exists already"))

    } recover {
      case e: ReactiveMongoException ⇒
        InternalServerError(e.getMessage)
    }
  }

  def loginUser = Action.async(parse.json) { request: Request[JsValue] ⇒
    val userPasswordOpt = (
      (request.body \ "username").asOpt[String],
      (request.body \ "password").asOpt[String])

    userPasswordOpt match {
      case (Some(username), Some(password)) ⇒
        import com.github.t3hnar.bcrypt._
        userDao.findUserByUsername(username).map {

          case Some(u:User) if (password.isBcrypted(u.password)) ⇒
            Ok(u.userRoles.mkString(",")).withSession(SessionKeys.USERNAME → u.username)
          case _ ⇒
            BadRequest("invalid username/password")
        }
      case _ ⇒
        Future.successful(BadRequest("username/password missing"))
    }
  }

  def restrictOne = deadbolt.Restrict(List(Array("ACT_MGR")))() { authRequest =>
    Future {
      Ok(accessOk())
    }
  }
}
