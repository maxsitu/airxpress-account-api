package dao

import java.time.format.DateTimeFormatter
import java.time.{Instant, OffsetDateTime, ZoneOffset}

import javax.inject.{Inject, Singleton}
import model.User
import reactivemongo.api.Cursor
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.{BSONDocumentReader, BSONDocumentWriter, BSONHandler, BSONString, Macros, document}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserDao @Inject() (mongoDb : MongoDB)
                        (implicit ec: ExecutionContext){

  implicit val dateTimeHandler = new BSONHandler[BSONString, OffsetDateTime] {
    override def read(bson: BSONString): OffsetDateTime = OffsetDateTime.ofInstant(Instant.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(bson.value)), ZoneOffset.UTC)
    override def write(t: OffsetDateTime): BSONString = BSONString(t.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneOffset.UTC)))
  }

  def userCollection: Future[BSONCollection] = mongoDb.axAccountDb.map(_.collection("user"))

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
