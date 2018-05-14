package security

import be.objectify.deadbolt.scala.cache.HandlerCache
import be.objectify.deadbolt.scala.{DeadboltHandler, HandlerKey}
import javax.inject.Singleton

@Singleton
class UserRoleHandlerCache extends HandlerCache {
  val defaultHandler: DeadboltHandler = new UserRoleDeadboltHandler

  override def apply(): DeadboltHandler = defaultHandler

  override def apply(v1: HandlerKey): DeadboltHandler = defaultHandler
}
