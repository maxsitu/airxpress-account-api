package model

import java.time.OffsetDateTime

import be.objectify.deadbolt.scala.models.{
  Subject,
  Permission ⇒ DeadboltPermission,
  Role ⇒ DeadboltRole
}

case class LoginUser(username: String, password: String)
case class RegisterUser(username: String,
                        firstName: String,
                        lastName: String,
                        dateOfBirth: String,
                        password: String,
                        email: String)
case class BasicUser(username: String,
                     firstName: String,
                     lastName: String,
                     dateOfBirth: String,
                     userRoles: List[UserRole])
case class SelfUpdatUser(firstName: String, lastName: String, dateOfBirth: String)

/**
* Class of user to be stored in database
  * @param username
  * @param firstName
  * @param lastName
  * @param dateOfBirth
  * @param userRoles
  * @param password
  * @param updatedOn
  * @param createdOn
  */
case class User(username: String,
                firstName: String,
                lastName: String,
                dateOfBirth: String,
                userRoles: Set[RoleName],
                password: String,
                updatedOn: OffsetDateTime,
                createdOn: OffsetDateTime)
object User {
  def apply(user: RichUser): User =
    new User(
      user.username,
      user.firstName,
      user.lastName,
      user.dateOfBirth,
      user.userRoles.map(_.name),
      user.password,
      user.updatedOn,
      user.createdOn
    )
}

/**
* RichUser contains complete user role information and to be used for deadbolt
  * @param username
  * @param firstName
  * @param lastName
  * @param dateOfBirth
  * @param userRoles
  * @param password
  * @param updatedOn
  * @param createdOn
  */
case class RichUser(
    username: String,
    firstName: String,
    lastName: String,
    dateOfBirth: String,
    userRoles: Set[UserRole],
    password: String,
    updatedOn: OffsetDateTime,
    createdOn: OffsetDateTime
) extends Subject {

  override def identifier: String = username

  override def roles: List[UserRole] = userRoles.toList

  override def permissions: List[DeadboltPermission] =
    userRoles
      .flatMap(_.permissions.map(p ⇒ new DeadboltPermission { override def value: String = p }))
      .toList
}

object RichUser {
  import play.api.libs.json._

  def apply(user: User, userRoles: Set[UserRole]): RichUser =
    new RichUser(
      user.username,
      user.firstName,
      user.lastName,
      user.dateOfBirth,
      userRoles,
      user.password,
      user.updatedOn,
      user.createdOn
    )

  implicit val userWrites: Writes[User] = Json.writes[User]

  implicit val richUserWrites: Writes[RichUser] = Json.writes[RichUser]

  implicit val loginUserReads: Reads[LoginUser] = Json.reads[LoginUser]

  implicit val selfUpdatUser: Reads[SelfUpdatUser] = Json.reads[SelfUpdatUser]

  implicit val registerUserReads: Reads[RegisterUser] = Json.reads[RegisterUser]
}

//case class UserPermission(val value: String)                                extends DeadboltPermission
case class UserRole(val name: RoleName, val permissions: Set[UserPermission]) extends DeadboltRole

object UserRole {
  import play.api.libs.json._

//  implicit val userPermissionWrites = Json.writes[UserPermission]
//  implicit val userPermissionReads  = Json.reads[UserPermission]
  implicit val userRoleWrites = Json.writes[UserRole]
  implicit val userRoleReads  = Json.reads[UserRole]
}
