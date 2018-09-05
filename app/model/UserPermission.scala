package model
import play.api.libs.json._

import be.objectify.deadbolt.scala.models.Permission

case class UserPermission(value: String) extends Permission

object UserPermission {
  implicit val userPermissionWrites = Json.writes[UserPermission]

  implicit val userPermissionReads = Json.reads[UserPermission]
}
