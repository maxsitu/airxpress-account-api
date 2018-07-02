package security

import akka.actor.ActorSystem
import be.objectify.deadbolt.scala.DeadboltExecutionContextProvider
import javax.inject.Inject

import scala.concurrent.ExecutionContext

/**
 * A custom execution context can be provided to Deadbolt for asynchronous operations.
 *
 * @author Steve Chaloner (steve@objectify.be)
 */
class CustomDeadboltExecutionContextProvider @Inject() (actorSystem: ActorSystem)
  extends DeadboltExecutionContextProvider {

  val deadboltExecutionContext = actorSystem.dispatchers.lookup("deadbolt-context")

  override def get(): ExecutionContext = deadboltExecutionContext
}
