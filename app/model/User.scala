package model

import java.time.OffsetDateTime

import be.objectify.deadbolt.scala.models.Subject

case class LoginUser(username: String, password: String)
case class RegisterUser(username: String,
                        firstName: String,
                        lastName: String,
                        dateOfBirth: String,
                        userRoles: Set[UserRole],
                        userPermissions: Set[UserPermission],
                        password: String,
                        email: String)
case class BasicUser(username: String,
                     firstName: String,
                     lastName: String,
                     dateOfBirth: String,
                     userRoles: List[UserRole],
                     userPermissions: Set[UserPermission])
case class User(username: String,
                firstName: String,
                lastName: String,
                dateOfBirth: String,
                userRoles: Set[UserRole],
                userPermissions: Set[UserPermission],
                password: String,
                updatedOn: OffsetDateTime,
                createdOn: OffsetDateTime)
    extends Subject {

  override def identifier: String = username

  override def roles: List[UserRole] = userRoles.toList

  override def permissions: List[UserPermission] = userPermissions.toList
}

object User {
  import play.api.libs.json._
  import play.api.libs.functional.syntax._

  implicit val userWrites: Writes[User] = Writes { u =>
    import UserPermission.userPermissionWrites
    import UserRole.userRoleWrites

    Json.obj(
      "username"    → u.username,
      "firstName"   → u.firstName,
      "lastName"    → u.lastName,
      "dateOfBirth" → u.dateOfBirth,
      "roles"       → u.userRoles,
      "permissions" → Json.toJson(u.userPermissions),
      "updatedOn"   → u.updatedOn,
      "createdOn"   → u.createdOn
    )
  }

  implicit val loginUserReads: Reads[LoginUser] = (
    (JsPath \ "username").read[String] and (JsPath \ "password").read[String]
  )(LoginUser.apply _)

  implicit val registerUserReads: Reads[RegisterUser] = Json.reads[RegisterUser]
}
