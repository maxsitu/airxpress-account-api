package model

import java.time.OffsetDateTime

import be.objectify.deadbolt.scala.models.Subject
import model.UserRole.UserRoleValue
import play.api.libs.json.{Json, Reads}

case class RegisterUser(
                         username: String, firstName: String, lastName: String, dateOfBirth: String,
                         userRoles: List[UserRoleValue], userPermissions: List[UserPermission], password: String,
                         email: String
                       )
object RegisterUser {
  implicit val RegisterUserReads: Reads[RegisterUser] = Json.reads[RegisterUser]
}
case class BasicUser(
                      username:String, firstName: String, lastName: String, dateOfBirth: String, userRoles: List[UserRoleValue],
                      userPermissions: List[UserPermission]
                    )
case class User(
                 username:String, firstName: String, lastName: String, dateOfBirth: String, userRoles: List[UserRoleValue],
                 userPermissions: List[UserPermission], password: String, updatedOn: OffsetDateTime,
                 createdOn: OffsetDateTime
               ) extends Subject {

  override def identifier: String = username
  override def roles: List[UserRoleValue] = userRoles
  override def permissions: List[UserPermission] = userPermissions
}

object User {
  import play.api.libs.json._

  implicit val userRoleValueWrites = UserRole.UserRoleValueWrites

  implicit object UserWrites extends OWrites[User] {
    override def writes(u: User): JsObject = Json.obj(
      "username"  → u.username,
      "firstName" → u.firstName,
      "lastName"  → u.lastName,
      "dateOfBirth" → u.dateOfBirth,
      "roles"     → u.userRoles,
      "permissions" → u.userPermissions,
      "password"  → u.password,
      "updatedOn" → u.updatedOn,
      "createdOn" → u.createdOn
    )
  }
}

