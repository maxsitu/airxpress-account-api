package model

import java.time.OffsetDateTime

import be.objectify.deadbolt.scala.models.Subject
import model.UserRole.UserRoleValue
import play.api.libs.json.{JsPath, Json, Reads}


case class LoginUser (username: String, password: String)
object LoginUser {
  import play.api.libs.functional.syntax._
  val loginUserReads: Reads[LoginUser] = (
    (JsPath \ "username").read[String] and
      (JsPath \ "password").read[String]
    )(LoginUser.apply _)
}

case class RegisterUser(
                         username: String, firstName: String, lastName: String, dateOfBirth: String,
                         userRoles: List[UserRoleValue], userPermissions: List[UserPermission], password: String,
                         email: String
                       )
object RegisterUser {
  implicit val permissionReads = UserPermission.userPermissionReads
  implicit val userRoleValueReads = UserRole.userRoleValueReads

  val registerUserReads: Reads[RegisterUser] = Json.reads[RegisterUser]
}

case class BasicUser
(
  username: String, firstName: String, lastName: String, dateOfBirth: String, userRoles: List[UserRoleValue],
  userPermissions: List[UserPermission]
)

case class User
(
  username: String, firstName: String, lastName: String, dateOfBirth: String, userRoles: List[UserRoleValue],
  userPermissions: List[UserPermission], password: String, updatedOn: OffsetDateTime, createdOn: OffsetDateTime
) extends Subject {

  override def identifier: String = username

  override def roles: List[UserRoleValue] = userRoles

  override def permissions: List[UserPermission] = userPermissions
}

object User {
  import play.api.libs.json._

  val userWrites: Writes[User] = Writes { u =>
    implicit val permissionWrites = UserPermission.userPermissionWrites

    Json.obj(
      "username"  → u.username,
      "firstName" → u.firstName,
      "lastName"  → u.lastName,
      "dateOfBirth" → u.dateOfBirth,
      "roles"     → u.userRoles,
      "permissions" → Json.toJson(u.userPermissions),
      "updatedOn" → u.updatedOn,
      "createdOn" → u.createdOn
    )
  }
}

