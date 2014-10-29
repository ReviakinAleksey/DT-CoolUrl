package com.cool.url.service

import scala.slick.lifted.ProvenShape


trait FoldersComponent {
  this: DbConnectorComponent
    with QueryExtensions
    with UsersComponent =>

  import connector.driver.simple._

  type FolderId = Long

  case class Folder(id: FolderId, token: UserToken, title: String)

  class Folders(tag: Tag) extends Table[Folder](tag, connector.schema, "folders") {
    def id = column[FolderId]("id", O.PrimaryKey, O.AutoInc)

    def token = column[UserToken]("user_token")

    def title = column[String]("title")

    override def * : ProvenShape[Folder] = (id, token, title) <>(Folder.tupled, Folder.unapply)

    def user = foreignKey("fK_folders_to_user", token, users)(_.token, onUpdate = ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.Cascade)


    def id_to_token_index = index("idx_folders_id_and_token", (id, token), unique = true)
  }

  object folders extends TableQuery(new Folders(_)) with PaginationExtension[Folders]{

    def createForToken(token: UserToken, title: String)(implicit session: Session): Folder = {
      val id = this.map(_.title).returning(this.map(_.id)).insert(title)
      Folder(id, token, title)
    }


    def folderByTokenAndId(token: UserToken, folderId: FolderId)(implicit session: Session): Folder = {
      this.filter(_.token === token).filter(_.id === folderId).firstOption.getOrElse(throw UserTokenUnknown(token, ValidationException.INTERNAL_ERROR))
    }


    def foldersByToken(token: UserToken, paging:Option[Paging])(implicit session: Session): PagingResult[Folder] = {
      this.filter(_.token === token).withPaging(paging)
    }


    def deleteByToken(token: UserToken)(implicit session: Session) = {
      this.filter(_.token === token).delete
    }
  }

}