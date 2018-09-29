package dao

import java.time.format.DateTimeFormatter
import java.time.{Instant, OffsetDateTime, ZoneOffset}

import javax.inject.{Inject, Singleton}
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.{
  BSONDocumentReader,
  BSONDocumentWriter,
  BSONHandler,
  BSONString,
  Macros,
  document
}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserDao @Inject()(mongoDb: MongoDB)(implicit ec: ExecutionContext) {

  import UserDao._
  import model._
  import reactivemongo.api.Cursor

  def userCollection: Future[BSONCollection] = mongoDb.axAccountDb.map(_.collection("user"))

  def createUser(richUser: RichUser): Future[Unit] =
    userCollection.flatMap(_.insert(User(richUser))).map(_ ⇒ ())

  def updateUser(richUser: RichUser): Future[Int] =
    userCollection.flatMap(
      _.update(
        document("username" -> richUser.username),
        User(richUser)
      ).map(_.n)
    )

  def findUserByUsername(username: String): Future[Option[User]] =
    userCollection.flatMap(
      collection ⇒
        collection
          .find(
            document("username" -> username)
          ).one[User])

  def findRichUserByUsername(username: String): Future[Option[RichUser]] =
    for {
      userOpt ← findUserByUsername(username)
      richUser ← userOpt.fold[Future[Option[RichUser]]](
        Future.successful(None)
      )(
        user ⇒
          findUserRoles(user.userRoles)
            .map(roles ⇒ Some(RichUser(user, roles))))
    } yield richUser

  def roleCollection: Future[BSONCollection] = mongoDb.axAccountDb.map(_.collection("role"))

  def findUserRole(roleName: RoleName): Future[Option[UserRole]] =
    roleCollection.flatMap(
      _.find(document("name" → roleName)).one[UserRole]
    )

  def findUserRoles(roleNames: Set[RoleName]): Future[Set[UserRole]] = {

    val result: Set[UserRole] = Set.empty[UserRole]
    Future.foldLeft(roleNames.map(roleName ⇒ findUserRole(roleName)))(result)((result, roleOpt) ⇒ {
      roleOpt match {
        case Some(role) ⇒ result + role
        case None       ⇒ result
      }
    })
  }

  def findAllUserRoles(): Future[Set[UserRole]] =
    roleCollection.flatMap(
      _.find(document()).cursor[UserRole]().collect[Set](-1, Cursor.FailOnError[Set[UserRole]]())
    )

  /**
    * Insert userRole into mongodb
    *
    * @param userRole
    * @return num of rows inserted.
    */
  def createRole(userRole: UserRole): Future[Int] =
    roleCollection.flatMap(_.insert(userRole)).map(_.n)

  def updateUserRole(userRole: UserRole): Future[Int] =
    roleCollection.flatMap(_.update(document("name" -> userRole.name), userRole).map(_.n))
}

object UserDao {
  import model.{RichUser, User, UserRole}
  implicit val dateTimeHandler = new BSONHandler[BSONString, OffsetDateTime] {
    override def read(bson: BSONString): OffsetDateTime =
      OffsetDateTime.ofInstant(
        Instant.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(bson.value)),
        ZoneOffset.UTC)
    override def write(t: OffsetDateTime): BSONString =
      BSONString(t.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneOffset.UTC)))
  }

  implicit def roleWriter: BSONDocumentWriter[UserRole] = Macros.writer[UserRole]
  implicit def roleReader: BSONDocumentReader[UserRole] = Macros.reader[UserRole]

  implicit def userWriter: BSONDocumentWriter[User] = Macros.writer[User]
  implicit def userReader: BSONDocumentReader[User] = Macros.reader[User]

  implicit def richUserWriter: BSONDocumentWriter[RichUser] = Macros.writer[RichUser]
  implicit def richUserReader: BSONDocumentReader[RichUser] = Macros.reader[RichUser]
}
