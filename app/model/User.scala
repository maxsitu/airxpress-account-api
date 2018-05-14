package model

import java.time.OffsetDateTime

import be.objectify.deadbolt.scala.models.{Permission, Subject}

case class RegisterUser(username: String, firstName: String, lastName: String, dateOfBirth: String, userRoles: Set[String], password: String, email: String)
case class BasicUser(username:String, firstName: String, lastName: String, dateOfBirth: String, userRoles: Set[String])

case class User(username:String, firstName: String, lastName: String, dateOfBirth: String,
  userRoles: Set[String], password: String, updatedOn: OffsetDateTime, createdOn: OffsetDateTime) extends Subject {

  override def identifier: String = username
  override def roles: List[SecurityRole] = userRoles.toList.map(SecurityRole(_))
  override def permissions: List[Permission] = List(UserPermission("user.edit"))
}

object User {
  import play.api.libs.json._

  implicit object UserWrites extends OWrites[User] {
    override def writes(u: User): JsObject = Json.obj(
      "username"  → u.username,
      "firstName" → u.firstName,
      "lastName"  → u.lastName,
      "dateOfBirth" → u.dateOfBirth,
      "roles"  → u.userRoles,
      "password"  → u.password,
      "updatedOn" → u.updatedOn,
      "createdOn" → u.createdOn
    )
  }
}

