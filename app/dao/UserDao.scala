package dao

import java.time.{Instant, OffsetDateTime, ZoneOffset}
import java.time.format.DateTimeFormatter
import java.util.TimeZone

import model.{BasicUser, User}
import org.joda.time.DateTimeZone
import play.api.libs.json.{Reads, Writes}
import reactivemongo.api.{Cursor, DefaultDB, MongoConnection, MongoDriver}
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.{BSONDocumentReader, BSONDocumentWriter, BSONHandler, BSONString, Macros, document}

import scala.concurrent.{ExecutionContext, Future}

object UserDao {
  implicit val dateTimeHandler = new BSONHandler[BSONString, OffsetDateTime] {
    override def read(bson: BSONString): OffsetDateTime = OffsetDateTime.ofInstant(Instant.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(bson.value)), ZoneOffset.UTC)
    override def write(t: OffsetDateTime): BSONString = BSONString(t.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneOffset.UTC)))
  }

  val mongoUri = "mongodb://localhost:27017/ax_account_db?authMode=scram-sha1"

  import ExecutionContext.Implicits.global // use any appropriate context

  // Connect to the database: Must be done only once per application
  val driver = MongoDriver()
  val parsedUri = MongoConnection.parseURI(mongoUri)
  val connection = parsedUri.map(driver.connection(_))

  // Database and collections: Get references
  val futureConnection = Future.fromTry(connection)
  def db1: Future[DefaultDB] = futureConnection.flatMap(_.database("ax_account_db"))
  def userCollection: Future[BSONCollection] = db1.map(_.collection("user"))

  // Write Documents: insert or update

  implicit def userWriter: BSONDocumentWriter[User] = Macros.writer[User]

  def createUser(user: User): Future[Unit] =
    userCollection.flatMap(_.insert(user).map(_ => {})) // use personWriter

  def updateUser(user: User): Future[Int] = {
    val selector = document(
      "username" -> user.username
    )

    // Update the matching person
    userCollection.flatMap(_.update(selector, user).map(_.n))
  }

  implicit def personReader: BSONDocumentReader[User] = Macros.reader[User]


  def findUserByUsername(username: String): Future[Option[User]] =
    userCollection.flatMap(_.find(document("username" -> username)).cursor[User]().collect[List](-1, Cursor.FailOnError[List[User]]()).map(_.headOption))
}
