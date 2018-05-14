package validator

import java.text.SimpleDateFormat

import model.SecurityRole

import scala.util.Try

sealed trait UserRegisterResult

object UserRegisterResult {
  case class InvalidData(login: Option[String], email: Option[String], password: Option[String]) extends UserRegisterResult
  case class UserExists(login: Option[String], email: Option[String]) extends UserRegisterResult
  case object Success extends UserRegisterResult
}

object UserRegisterValidator {
  val DOBFormat         = new SimpleDateFormat("yyyyMMdd")
  val ValidationOk      = Right((): Unit)
  val MinUsernameLength = 3
  val emailRegex =
    """^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$""".r

  def validateUsername(username: String) =
    if (username.length >= UserRegisterValidator.MinUsernameLength) UserRegisterValidator.ValidationOk else Left("username is too short")

  def validateEmail(email: String) =
    if (UserRegisterValidator.emailRegex.findFirstMatchIn(email).isDefined) UserRegisterValidator.ValidationOk else Left("invalid e-mail")

  def validatePassword(password: String) =
    if (password.nonEmpty) UserRegisterValidator.ValidationOk else Left("password cannot be empty")

  def validateDateOfBirth(dob: String) =
    Try(DOBFormat.parse(dob)).fold(_ ⇒ Left("invalid date of birth"), _ ⇒ UserRegisterValidator.ValidationOk)

  def validateUserRoles(roles: Set[String]): Either[String, String] =
    roles.map(role ⇒ SecurityRole.parseRole(role)).foldLeft[Either[Unit, String]](Right(""))(
      (result, role) ⇒ result.flatMap(r ⇒ role)
    ) match {
      case Left(()) ⇒ Left("invalid role")
      case r @ Right(str) ⇒ r.asInstanceOf[Either[String, String]]
    }
}
