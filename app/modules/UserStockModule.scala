package modules

import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport

class UserStockModule extends AbstractModule with AkkaGuiceSupport {
  import actors._

  override def configure(): Unit = {
    bindActor[UserParentActor]("userParentActor")
    bindActor[ActiveMqActor]("activeMqActor")
  }
}

