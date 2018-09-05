package dao

import java.time.format.DateTimeFormatter
import java.time.{Instant, OffsetDateTime, ZoneOffset}

import javax.inject.{Inject, Singleton}
import model.{User, UserPermission}
import reactivemongo.api.Cursor
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.{BSONDocumentReader, BSONDocumentWriter, BSONHandler, BSONString, Macros, document}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserDao @Inject() (mongoDb : MongoDB)
                        (implicit ec: ExecutionContext){

  import UserDao._

  def userCollection: Future[BSONCollection] = mongoDb.axAccountDb.map(_.collection("user"))

  def createUser(user: User): Future[Unit] =
    userCollection.flatMap(_.insert(user).map(_ => {})) // use personWriter

  def updateUser(user: User): Future[Int] = {
    val selector = document(
      "username" -> user.username
    )

    userCollection.flatMap(_.update(selector, user).map(_.n))
  }

  def findUserByUsername(username: String): Future[Option[User]] =
    userCollection.flatMap(_.find(document("username" -> username)).cursor[User]().collect[List](-1, Cursor.FailOnError[List[User]]()).map(_.headOption))
}

object UserDao {
  import model.UserRole
  implicit val dateTimeHandler = new BSONHandler[BSONString, OffsetDateTime] {
    override def read(bson: BSONString): OffsetDateTime = OffsetDateTime.ofInstant(Instant.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(bson.value)), ZoneOffset.UTC)
    override def write(t: OffsetDateTime): BSONString = BSONString(t.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneOffset.UTC)))
  }

  implicit val userRoleHandler = new BSONHandler[BSONString, UserRole] {
    override def write(t: UserRole): BSONString = BSONString(t.name)
    override def read(bson: BSONString): UserRole = model.UserRole.parse(bson.value).getOrElse(model.VoidUser)
  }

  implicit val userPermissionHandler = new BSONHandler[BSONString, UserPermission] {
    override def write(t: UserPermission): BSONString = BSONString(t.value)
    override def read(bson: BSONString): UserPermission = UserPermission(bson.value)
  }

  implicit def userWriter: BSONDocumentWriter[User] = Macros.writer[User]

  implicit def personReader: BSONDocumentReader[User] = Macros.reader[User]
}
