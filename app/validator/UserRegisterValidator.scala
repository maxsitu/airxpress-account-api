package validator

import java.text.SimpleDateFormat
import scala.util.Try
import com.github.t3hnar.bcrypt._

object UserRegisterValidator {
  import constant.ValidationConstant.RegisterUserValidationMsg._
  import model.{RegisterUser, RichUser}

  val DOBFormat         = new SimpleDateFormat("yyyyMMdd")
  val MinUsernameLength = 3
  val MinFirstnameLength = 2
  val MinLastnameLength = 2
  val emailRegex =
    """^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$""".r

  def validateUsername(username: String) =
    if (username.length >= MinUsernameLength)
      Right(username)
    else Left(ERR_MSG_USERNAME_TOO_SHORT)

  def validateFirstname(firstname: String) =
    if (firstname.length >= MinFirstnameLength)
      Right(firstname)
    else Left(ERR_MSG_FIRSTNAME_INVALID)

  def validateLastname(lastname: String) =
    if (lastname.length >= MinLastnameLength)
      Right(lastname)
    else Left(ERR_MSG_LASTNAME_INVALID)

  def validateEmail(email: String) =
    if (UserRegisterValidator.emailRegex.findFirstMatchIn(email).isDefined)
      Right(email)
    else Left(ERR_MSG_EMAIL_FORMAT_INVALID)

  def validatePassword(password: String) =
    if (password.nonEmpty)
      Right(password)
    else Left(ERR_MSG_PASSWORD_EMPTY)

  def validateDateOfBirth(dob: String) =
    Try(DOBFormat.parse(dob)).fold(
      _ ⇒ Left(ERR_MSG_DATE_OF_BIRTH_INVALID),
      _ ⇒ Right(dob)
    )
}