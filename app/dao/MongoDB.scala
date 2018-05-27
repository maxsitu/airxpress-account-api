package dao

import javax.inject._
import play.api.libs.concurrent.ExecutionContextProvider
import reactivemongo.api.{DefaultDB, MongoConnection, MongoDriver}

import scala.concurrent.Future

@Singleton
class MongoDB @Inject() (ecProvider: ExecutionContextProvider) {

  implicit val ec = ecProvider.get()

  val mongoUri = "mongodb://localhost:27017/ax_account_db?authMode=scram-sha1"

  // Connect to the database: Must be done only once per application
  val driver = MongoDriver()
  val parsedUri = MongoConnection.parseURI(mongoUri)
  val connection = parsedUri.map(driver.connection(_))

  // Database and collections: Get references
  val futureConnection = Future.fromTry(connection)
  def axAccountDb: Future[DefaultDB] = futureConnection.flatMap(_.database("ax_account_db"))
}
