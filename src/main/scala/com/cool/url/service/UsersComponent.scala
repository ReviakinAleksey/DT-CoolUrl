package com.cool.url.service

import scala.slick.lifted.{Index, ProvenShape}


trait UsersComponent {
  this: DbConnectorComponent
    with TokenServiceComponent =>

  import connector.driver.simple._

  type UserToken = String

  case class User(id: Long, token: UserToken)

  class Users(tag: Tag) extends Table[User](tag, connector.schema, "users") {
    def id: Column[Long] = column[Long]("id")

    def token: Column[UserToken] = column[UserToken]("token", O.PrimaryKey)

    override def * : ProvenShape[User] = (id, token) <>(User.tupled, User.unapply)


    def tokenIndex:Index = index("idx_user_token", token, unique = true)
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

