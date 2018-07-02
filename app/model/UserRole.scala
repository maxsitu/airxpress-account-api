package model
import be.objectify.deadbolt.scala.models.Role
import play.api.libs.json._

object UserRole extends Enumeration {
  case class UserRoleValue(name: String) extends Val with Role

  implicit def valueToUserRoleValue(x: Value): UserRoleValue = x.asInstanceOf[UserRoleValue]

  val Void    = Value("Void")
  val Normal  = Value("Normal")
  val Admin   = Value("Admin")
  val AcctMgr = Value("AcctMgr")

  def parse(s: String): Either[Unit, UserRoleValue] = UserRole.values.find(_.name == s) match {
    case Some(value)  ⇒ Right(value)
    case None         ⇒ Left(())
  }

  implicit object UserRoleValueWrites extends Writes[UserRoleValue] {
    override def writes(o: UserRoleValue): JsValue = JsString(o.name)
  }

  implicit object UserRoleValueReads extends Reads[UserRoleValue] {
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
}

