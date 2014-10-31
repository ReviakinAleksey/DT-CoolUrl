package com.cool.url.service


import com.cool.url.service

import scala.slick.lifted.{Index, ForeignKeyQuery, ProvenShape}
import ParametersValidation._

object LinksComponent {
  private val USER_CODE_CONSTRAINT = "fK_links_to_user"
  private val FOLDER_ID_CONSTRAINT = "fk_links_to_folder"
  private val LINK_CODE_MAX_LENGTH = 128
  private val LINK_URL_MAX_LENGTH = 2048

  val linkCodeIsValid = beNonEmpty and beTrimmed and hasLengthLE(LinksComponent.LINK_CODE_MAX_LENGTH) and beValidPath
}


trait LinksComponent {
  this: DbConnectorComponent
    with QueryExtensions
    with UsersComponent
    with ClicksComponent
    with FoldersComponent =>

  import connector.driver.simple._

  type LinkCode = String

  case class Link(code: LinkCode, token: UserToken, url: String, folder: Option[Long])

  trait LinkedWithLink {
    self: Table[_] =>
    def linkCode: Column[LinkCode] = column[LinkCode]("link_code", O.Length(LinksComponent.LINK_CODE_MAX_LENGTH, true))
  }

  class Links(tag: Tag) extends Table[Link](tag, connector.schema, "links") with LinkedToUser with LinkedWithFolder {
    def code: Column[LinkCode] = column[LinkCode]("code", O.PrimaryKey, O.Length(LinksComponent.LINK_CODE_MAX_LENGTH, true))

    def url: Column[String] = column[String]("url", O.Length(LinksComponent.LINK_URL_MAX_LENGTH, true))

    override def * : ProvenShape[Link] = (code, token, url, folderId) <>(Link.tupled, Link.unapply)

    def user: ForeignKeyQuery[Users, User] =
      foreignKey(LinksComponent.USER_CODE_CONSTRAINT, token, users)(_.token,
        onUpdate = ForeignKeyAction.Restrict,
        onDelete = ForeignKeyAction.Cascade)

    def folder: ForeignKeyQuery[Folders, Folder] =
      foreignKey(LinksComponent.FOLDER_ID_CONSTRAINT, (folderId, token), folders)(folder => (folder.id, folder.token),
        onUpdate = ForeignKeyAction.Restrict,
        onDelete = ForeignKeyAction.Cascade)

    def tokenIndex: Index = index("idx_links_token", token)

    def folderIdIndex: Index = index("idx_links_folder", folderId)
  }


  object links extends TableQuery(new Links(_)) with PaginationExtension[Links] {


    def create(token: UserToken, url: String, code: Option[LinkCode], folderId: Option[FolderId])(implicit session: Session): Link = {

      for {
        _ <- token validateAs "link.token" ensure UsersComponent.beVaildToken
        _ <- url validateAs "link.url" ensure (hasLengthLE(LinksComponent.LINK_URL_MAX_LENGTH) and beURL)
      } yield {
        try {
          val dbCode: String = code match {
            case Some(enteredCode) =>
              for {
                _ <- enteredCode validateAs "link.code" ensure LinksComponent.linkCodeIsValid
              } yield {
                (this returning this.map(_.code)) += Link(enteredCode, token, url, folderId)
              }
            case _ =>
              (this.map(l => (l.token, l.url, l.folderId)) returning this.map(_.code)).insert(token, url, folderId)
          }
          Link(dbCode, token, url, folderId)
        } catch {
          case connector.UNIQUE_VIOLATION(constraint) =>
            (constraint, folderId, code) match {
              case (LinksComponent.USER_CODE_CONSTRAINT, _, _) => throw UserTokenUnknown(token, ValidationException.FORBIDDEN)
              case (LinksComponent.FOLDER_ID_CONSTRAINT, Some(folder), _) => throw FolderIsNotExists(folder, ValidationException.INTERNAL_ERROR)
              case (_, _, Some(requestedCode)) => throw LinkCodeIsAlreadyOccupied(requestedCode, ValidationException.INTERNAL_ERROR)
              case (_, _, None) =>
                //Race conditions, re-request new code
                create(token, url, code, folderId)
            }
        }
      }
    }

    def linkByCode(code: LinkCode)(implicit session: Session): Link =
      this.filter(_.code === code).firstOption.getOrElse(throw LinkDoesNotExists(code, ValidationException.NOT_FOUND))

    def linkByCodeAndToken(token: UserToken, code: LinkCode)(implicit session: Session): Link =
      this.filter(_.code === code).filter(_.token === token).firstOption.getOrElse(throw LinkDoesNotExists(code, ValidationException.FORBIDDEN))

    def linksByToken(token: UserToken, paging: Option[Paging] = None)(implicit session: Session): PagingResult[Link] = {
      this.filter(_.token === token).withPaging(paging)
    }


    def linksByFolder(folderId: FolderId, paging: Option[Paging] = None)(implicit session: Session): PagingResult[Link] = {
      this.filter(_.folderId === folderId).withPaging(paging)
    }

    def linkAndClicksCountByCode(token: UserToken, code: LinkCode)(implicit session: Session): (Link, Int) = {
      val query = for {
        link <- this if link.token === token && link.code === code
        count <- clicks.filter(_.linkCode === code).groupBy(_.linkCode).map({
          case (linkCode, click) => click.length
        })
      } yield {
        (link, count)
      }
      query.firstOption.getOrElse(throw LinkDoesNotExists(code, ValidationException.NOT_FOUND))
    }


    def deleteByToken(token: UserToken)(implicit session: Session): Int = {
      this.filter(_.token === token).delete
    }


    val codeSequenceDDL: slickDriver.type#DDL = {
      val prefix = connector.schema.map(_ + ".").getOrElse("")
      slickDriver.DDL(
        Seq(
          s"create sequence ${prefix}link_code_seq",
          s"""
          |alter table ${prefix}links
          | alter column code set default 'lk' || nextval('${prefix}link_code_seq')""".stripMargin)
        ,
        Seq(s"alter table ${prefix}links alter column code set default ''",
          s"DROP SEQUENCE ${prefix}link_code_seq"))
    }
  }

}
