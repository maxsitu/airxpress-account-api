package model

import java.time.OffsetDateTime

case class RegisterUser(username: String, firstName: String, lastName: String, dateOfBirth: String, role: String, password: String, email: String)
case class BasicUser(username:String, firstName: String, lastName: String, dateOfBirth: String, role: String)
case class User(username:String, firstName: String, lastName: String, dateOfBirth: String, role: String, password: String, updatedOn: OffsetDateTime, createdOn: OffsetDateTime)
case class AuthInfo(username:String, password: String, sal: String, updatedOn: OffsetDateTime, createdOn: OffsetDateTime)

case object Role {
  def parseRole(roleStr: String): Either[Unit,Role] = roleStr match  {
    case "NORMAL" ⇒ Right(NormalRole)
    case "ADMIN"  ⇒ Right(AdminRole)
    case "ACCTMGR" ⇒ Right(AccountManagerRole)
    case _ ⇒ Left(())
  }
}

sealed trait Role {def name: String}
case object NormalRole extends Role {val name = "NORMAL"}
case object AdminRole extends Role {val name = "ADMIN"}
case object AccountManagerRole extends Role {val name = "ACCTMGR"}