package com.cool.url.service

import scala.slick.lifted.{ForeignKeyQuery, Index, ProvenShape}
import ParametersValidation._


object  FoldersComponent {
  private val USER_CODE_CONSTRAINT = "fK_folders_to_user"
  private val FOLDER_TO_USER_CODE_INDEX = "idx_folders_id_and_token"
  private val FOLDER_TITLE_TO_USER_CODE_INDEX = "idx_folders_token_and_title"
}

trait FoldersComponent {
  this: DbConnectorComponent
    with QueryExtensions
    with UsersComponent =>

  import connector.driver.simple._

  type FolderId = Long

  case class Folder(id: FolderId, token: UserToken, title: String)

  class Folders(tag: Tag) extends Table[Folder](tag, connector.schema, "folders") with LinkedToUser {

    def id:Column[FolderId] = column[FolderId]("id", O.PrimaryKey, O.AutoInc)

    def title: Column[String] = column[String]("title")

    override def * : ProvenShape[Folder] = (id, token, title) <>(Folder.tupled, Folder.unapply)

    def user: ForeignKeyQuery[Users, User] =
      foreignKey(FoldersComponent.USER_CODE_CONSTRAINT, token, users)(_.token,
        onUpdate = ForeignKeyAction.Restrict,
        onDelete = ForeignKeyAction.Cascade)


    def idToTokenIndex:Index = index(FoldersComponent.FOLDER_TO_USER_CODE_INDEX, (id, token), unique = true)

    def uniqueFolderNameIndex: Index = index(FoldersComponent.FOLDER_TITLE_TO_USER_CODE_INDEX, (token, title), unique = true)

    def folderIndex: Index = index("idx_folders_token", token)
  }

  trait LinkedWithFolder {
    self: Table[_] =>
    def folderId:Column[Option[FolderId]] = column[Option[FolderId]]("id_folder")
  }

  object folders extends TableQuery(new Folders(_)) with PaginationExtension[Folders] {

    def createForToken(token: UserToken, title: String)(implicit session: Session): Folder = {
      for {
        _ <- token validateAs  "folder.token" ensure UsersComponent.beVaildToken
        _ <- title validateAs "folder.title" ensure (beNonEmpty and beTrimmed)
      } yield {
        try {
          val id = this.map(folder => (folder.token, folder.title)).returning(this.map(_.id)).insert((token, title))
          Folder(id, token, title)
        } catch {
          case connector.UNIQUE_VIOLATION(FoldersComponent.USER_CODE_CONSTRAINT) =>
            throw UserTokenUnknown(token, ValidationException.FORBIDDEN)
          case connector.UNIQUE_VIOLATION(FoldersComponent.FOLDER_TITLE_TO_USER_CODE_INDEX) =>
            throw FolderAlreadyExists(title, ValidationException.INTERNAL_ERROR)
        }
      }
    }


    def folderByTokenAndId(token: UserToken, folderId: FolderId)(implicit session: Session): Folder = {
      this.filter(_.token === token).filter(_.id === folderId).firstOption.getOrElse(throw UserTokenUnknown(token, ValidationException.FORBIDDEN))
    }


    def foldersByToken(token: UserToken, paging: Option[Paging])(implicit session: Session): PagingResult[Folder] = {
      this.filter(_.token === token).withPaging(paging)
    }


    def deleteByToken(token: UserToken)(implicit session: Session):Int = {
      this.filter(_.token === token).delete
    }
  }

}
