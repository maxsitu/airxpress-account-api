package security

import be.objectify.deadbolt.scala.cache.HandlerCache
import be.objectify.deadbolt.scala.{DeadboltHandler, HandlerKey}
import javax.inject.{Inject, Singleton}

@Singleton
class UserRoleHandlerCache @Inject() (userRoleDeadboltHandler: UserRoleDeadboltHandler) extends HandlerCache {
  val defaultHandler: DeadboltHandler = userRoleDeadboltHandler

  override def apply(): DeadboltHandler = defaultHandler

  override def apply(v1: HandlerKey): DeadboltHandler = defaultHandler
}
