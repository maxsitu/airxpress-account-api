package model

import be.objectify.deadbolt.scala.models.{Role ⇒ DeadboltRole}

sealed abstract case class UserRole(val name: String) extends DeadboltRole

object NormalUser  extends UserRole(name = "Normal")
object AdminUser   extends UserRole(name = "Admin")
object AcctMgrUser extends UserRole(name = "AcctMgr")
object VoidUser    extends UserRole(name = "Void")

object UserRole {
  import play.api.libs.json._

  def parse(s: String): Option[UserRole] = {
    List(NormalUser, AdminUser, AcctMgrUser, VoidUser).find(_.name == s)
  }

  implicit def userRoleName(userRole: UserRole): String = userRole.name

  implicit val userRoleWrites = new Writes[UserRole] {
    override def writes(
        o: UserRole
    ): JsValue = JsString(o.name)
  }

  implicit val userRoleReads = new Reads[UserRole] {
    override def reads(
        json: JsValue
    ): JsResult[UserRole] = json match {
      case JsString(str: String) ⇒
        parse(str) match {
          case Some(value: UserRole) ⇒ JsSuccess(value)
          case None ⇒
            JsError(
              s"expected of type: '${UserRole.getClass}', but it does not appear to contain the value: '$str'"
            )
        }
      case _ ⇒ JsError("")
    }
  }
}
