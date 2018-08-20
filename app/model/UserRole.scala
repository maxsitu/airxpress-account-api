package model



object UserRole extends Enumeration {
  import be.objectify.deadbolt.scala.models.Role
  import play.api.libs.json._

  case class UserRoleValue(name: String) extends Val(name) with Role

  val Void    = UserRoleValue("Void")
  val Normal  = UserRoleValue("Normal")
  val Admin   = UserRoleValue("Admin")
  val AcctMgr = UserRoleValue("AcctMgr")

  def parse(s: String): Either[Unit, UserRoleValue] = UserRole.values.find(_.asInstanceOf[UserRoleValue].name == s) match {
    case Some(value)  ⇒ Right(value.asInstanceOf[UserRoleValue])
    case None         ⇒ Left(())
  }

  val userRoleValueWrites = Json.writes[UserRoleValue]

  object UserRoleValueReads extends Reads[UserRoleValue] {
    override def reads(json: JsValue): JsResult[UserRoleValue] = json match {
      case JsString(str: String) ⇒ UserRole.parse(str) match {
        case Right(value: UserRoleValue) ⇒ JsSuccess(value)
        case Left(()) ⇒
          JsError(
            s"Enumeration expected of type: '${UserRole.getClass}', but it does not appear to contain the value: '$str'"
          )
      }
      case  _ => JsError("String value expected")
    }
  }

  val userRoleValueReads = Json.reads[UserRoleValue]
}

