package modules

import be.objectify.deadbolt.scala.DeadboltExecutionContextProvider
import be.objectify.deadbolt.scala.cache.HandlerCache
import play.api.inject.{Binding, Module}
import play.api.{Configuration, Environment}
import security.{CustomDeadboltExecutionContextProvider, UserRoleHandlerCache}

class CustomDeadboltHook extends Module {
  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = Seq(
    bind[HandlerCache].to[UserRoleHandlerCache],
    bind[DeadboltExecutionContextProvider].to[CustomDeadboltExecutionContextProvider]
  )
}
