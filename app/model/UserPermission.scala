package model

import be.objectify.deadbolt.scala.models.Permission

case class UserPermission(value: String) extends Permission

object UserPermission {
  import play.api.libs.json._

  implicit object UserPermissionWrites extends Writes[UserPermission] {
    override def writes(o: UserPermission): JsValue = JsString(o.value)
  }

  implicit object UserPermissionReads extends Reads[UserPermission] {
    override def reads(json: JsValue): JsResult[UserPermission] = json match {
      case JsString(str: String) ⇒ JsSuccess(UserPermission(str))
      case _ ⇒ JsError("String value expected")
    }
  }
}
