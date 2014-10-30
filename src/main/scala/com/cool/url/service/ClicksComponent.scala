package com.cool.url.service

import java.sql.Timestamp

import scala.slick.lifted.{ForeignKeyQuery, Index, ProvenShape}

object ClicksComponent{
  private val LINK_CODE_CONSTRAINT = "fk_clicks_to_link"
}

trait ClicksComponent {
  this: DbConnectorComponent
    with QueryExtensions
    with LinksComponent =>

  import connector.driver.simple._


  case class Click(linkCode: LinkCode, date: Timestamp, referer: String, remoteIp: String)

  class Clicks(tag: Tag) extends Table[Click](tag, connector.schema, "clicks") {

    def linkCode:Column[LinkCode] = column[LinkCode]("link_code")

    def date:Column[Timestamp] = column[Timestamp]("date")

    def referer:Column[String] = column[String]("referer")

    def remoteIp:Column[String] = column[String]("remote_ip")

    override def * : ProvenShape[Click] = (linkCode, date, referer, remoteIp) <>(Click.tupled, Click.unapply)

    def link:ForeignKeyQuery[Links, Link] =
      foreignKey(ClicksComponent.LINK_CODE_CONSTRAINT, linkCode, links)(_.code,
        onUpdate = ForeignKeyAction.Restrict,
        onDelete = ForeignKeyAction.Cascade)

    def linkCodeIndex:Index = index("idx_clicks_link_code", linkCode)
  }

  object clicks extends TableQuery(new Clicks(_)) with PaginationExtension[Clicks] {

    def addClickForCode(code: LinkCode, date: Timestamp, referer: String, remoteIp: String)(implicit session: Session): Click = {
      try {
        val click = Click(code, date, referer, remoteIp)
        this += click
        click
      } catch {
        case connector.UNIQUE_VIOLATION(ClicksComponent.LINK_CODE_CONSTRAINT) =>
          throw LinkDoesNotExists(code, ValidationException.NOT_FOUND)
      }
    }

    def linksForCode(code: LinkCode, paging: Option[Paging] = None)(implicit session: Session): PagingResult[Click] = {
      this.filter(_.linkCode === code).withPaging(paging)
    }

    def deleteByCode(code: LinkCode)(implicit session: Session):Int = {
      this.filter(_.linkCode === code).delete
    }
  }

}
