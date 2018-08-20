package model

import model.UserRole.UserRoleValue
import play.api.libs.json.{Reads, Writes}

trait ModelJsonImplicits {
  implicit val loginUserReads: Reads[LoginUser] = LoginUser.loginUserReads
  implicit val userRoleValueReads: Reads[UserRoleValue] = UserRole.UserRoleValueReads
  implicit val userRoleValueWrites: Writes[UserRoleValue] = UserRole.userRoleValueWrites
  implicit val userPermissionReads: Reads[UserPermission] = UserPermission.userPermissionReads
  implicit val userPermissionWrites: Writes[UserPermission] = UserPermission.userPermissionWrites
  implicit val registerUserReads: Reads[RegisterUser] = RegisterUser.registerUserReads
  implicit val userWrites: Writes[User] = User.userWrites
}
