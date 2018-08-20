package model

import be.objectify.deadbolt.scala.models.Permission

case class UserPermission(value: String) extends Permission

object UserPermission {
  import play.api.libs.json._


  val userPermissionWrites = Json.writes[UserPermission]

  val userPermissionReads = Json.reads[UserPermission]
}
