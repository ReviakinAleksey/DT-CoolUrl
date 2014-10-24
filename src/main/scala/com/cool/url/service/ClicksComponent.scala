package com.cool.url.service

import java.sql.Timestamp

import scala.slick.lifted.ProvenShape


trait ClicksComponent {
  this: DbConnectorComponent
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
  }

  object clicks extends TableQuery(new Clicks(_))

}