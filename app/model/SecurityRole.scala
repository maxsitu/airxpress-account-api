package model

import be.objectify.deadbolt.scala.models.Role

case class SecurityRole(val name: String) extends Role

case object SecurityRole {
  val NORMAL = "NORMAL"
  val ADMIN  = "ADMIN"
  val ACCT_MGR = "ACCT_MGR"

  def parseRole(roleStr: String): Either[Unit,String] = roleStr match  {
    case role @ (`NORMAL` | `ADMIN` | `ACCT_MGR`) ⇒ Right(role)
    case _ ⇒ Left(())
  }
}

import model.SecurityRole._
object NormalRole extends SecurityRole(NORMAL)
object AdminRole extends SecurityRole (ADMIN)
object AccountManagerRole extends SecurityRole (ACCT_MGR)
