package com.cool.url.service

import java.sql.Timestamp

import scala.slick.lifted.ProvenShape


trait ClicksComponent {
  this: DbConnectorComponent
    with QueryExtensions
    with LinksComponent =>

  import connector.driver.simple._

  case class Click(linkCode: LinkCode, date: Timestamp, referer: String, remote_ip: String)

  class Clicks(tag: Tag) extends Table[Click](tag, connector.schema, "clicks") {
    def linkCode = column[LinkCode]("link_code")

    def date = column[Timestamp]("date")

    def referer = column[String]("referer")

    def remote_ip = column[String]("remote_ip")

    override def * : ProvenShape[Click] = (linkCode, date, referer, remote_ip) <>(Click.tupled, Click.unapply)

    def link = foreignKey("fk_clicks_to_link", linkCode, links)(_.code, onUpdate = ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.Cascade)

    def linkCodeIndex = index("link_code_index", linkCode)
  }

  object clicks extends TableQuery(new Clicks(_)) with PaginationExtension[Clicks] {
    def addClickForCode(code: LinkCode, date: Timestamp, referer: String, remote_ip: String)(implicit session: Session): Click = {
      val click = Click(code, date,referer, remote_ip)
      this += click
      click
    }

    def linksForCode(code:LinkCode, paging:Option[Paging] = None)(implicit session: Session):PagingResult[Click] = {
        this.filter(_.linkCode === code).withPaging(paging)
    }
  }

}