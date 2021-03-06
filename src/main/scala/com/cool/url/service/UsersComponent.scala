package com.cool.url.service

import scala.slick.lifted.{Index, ProvenShape}
import ParametersValidation._

object UsersComponent {
  private val TOKEN_COLUMN_LENGTH = 36

  val beVaildToken = hasLengthEQ(TOKEN_COLUMN_LENGTH)
}


trait UsersComponent {
  this: DbConnectorComponent
    with TokenServiceComponent =>

  import connector.driver.simple._

  type UserToken = String

  case class User(id: Long, token: UserToken)

  class Users(tag: Tag) extends Table[User](tag, connector.schema, "users") {
    def id: Column[Long] = column[Long]("id")

    def token: Column[UserToken] = column[UserToken]("token", O.PrimaryKey, O.Length(UsersComponent.TOKEN_COLUMN_LENGTH, false))

    override def * : ProvenShape[User] = (id, token) <>(User.tupled, User.unapply)


    def userIdIndex: Index = index("idx_users_id", id, unique = true)
  }


  trait LinkedToUser {
    self: Table[_] =>

    def token: Column[UserToken] = column[UserToken]("user_token", O.Length(UsersComponent.TOKEN_COLUMN_LENGTH, false))
  }


  object users extends TableQuery(new Users(_)) {

    def getToken(userId: Long)(implicit session: Session): UserToken = {
      this.filter(_.id === userId).firstOption.map(_.token).getOrElse({
        val token = tokenService.generate(userId)
        try {
          this += User(userId, token)
        } catch {
          case connector.UNIQUE_VIOLATION(_) =>
          //Concurrent creation.Drop this exception.
        }
        token
      })
    }

    def getByToken(token: UserToken)(implicit session: Session): Option[User] = this.filter(_.token === token).firstOption

  }


}

