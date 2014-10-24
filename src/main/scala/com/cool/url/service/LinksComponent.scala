package com.cool.url.service


import scala.slick.lifted.ProvenShape


trait LinksComponent {
  this: DbConnectorComponent
    with QueryExtensions
    with UsersComponent
    with FoldersComponent =>

  import connector.driver.simple._

  type LinkCode = String

  case class Link(code: LinkCode, token: UserToken, url: String, folder: Option[Long])

  val USER_CODE_CONSTRAINT = "fK_links_to_user"
  val FOLDER_ID_CONSTRAINT = "fk_links_to_folder"

  class Links(tag: Tag) extends Table[Link](tag, connector.schema, "links") {
    def code = column[LinkCode]("code", O.PrimaryKey)

    def token = column[UserToken]("user_token")

    def url = column[String]("url")

    def folderId = column[Option[Long]]("id_folder")

    override def * : ProvenShape[Link] = (code, token, url, folderId) <>(Link.tupled, Link.unapply)

    def user = foreignKey(USER_CODE_CONSTRAINT, token, users)(_.token, onUpdate = ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.Cascade)

    def folder = foreignKey(FOLDER_ID_CONSTRAINT, (folderId, token), folders)(folder => (folder.id, folder.token), onUpdate = ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.Cascade)

    //TODO: add index on folders
  }


  object links extends TableQuery(new Links(_)) with PaginationExtension[Links] {

    def create(token: UserToken, url: String, code: Option[String], folderId: Option[Long])(implicit session: Session): Link = {
      try {
        val dbCode: String = code match {
          case Some(enteredCode) =>
            (this returning this.map(_.code)) += Link(enteredCode, token, url, folderId)
          case _ =>
            (this.map(l => (l.token, l.url, l.folderId)) returning this.map(_.code)).insert(token, url, folderId)
        }
        Link(dbCode, token, url, folderId)
      } catch {
        case connector.UNIQUE_VIOLATION(constraint) =>
          (constraint, folderId, code) match {
            case (USER_CODE_CONSTRAINT, _, _) => throw UserTokenUnknown(token)
            case (FOLDER_ID_CONSTRAINT, Some(folder), _) => throw FolderIsNotExists(folder)
            case (_, _, Some(requestedCode)) => throw LinkCodeIsAlreadyOccupied(requestedCode)
            case (_, _, None) =>
              //Race conditions, re-request new code
              create(token, url, code, folderId)
          }
      }
    }

    def linkByCode(code: String)(implicit session: Session): Option[Link] = this.filter(_.code === code).firstOption

    def linksByToken(token: UserToken, paging: Option[Paging])(implicit session: Session) = {
      this.filter(_.token === token).withPaging(paging)
    }


    def linksByFolderQuery(folderId: Long, paging: Option[Paging])(implicit session: Session) = {
      this.filter(_.folderId === folderId).withPaging(paging)
    }


    def deleteAllByToken(token: String)(implicit session: Session): Int = {
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