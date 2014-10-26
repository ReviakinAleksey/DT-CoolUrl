package com.cool.url.service

import java.sql.Timestamp

import com.cool.url.BaseSpec
import org.scalatest.BeforeAndAfterAll
import org.scalatest.Matchers._


trait ClicksComponentFixture extends UsersComponentFixture with LinksComponentFixture {
  self: BaseSpec =>

  import context._
  import context.connector.driver.simple._

  val codeToClicks = collection.mutable.Map.empty[LinkCode, List[Click]]

  override def clearData(implicit session: Session) = {
    codeToLink.keys.foreach(clicks.deleteByCode(_))
    super.clearData(session)
  }
}


class ClicksComponentSpec extends BaseSpec with ClicksComponentFixture with BeforeAndAfterAll {

  import context._

  connector.db.withSession {
    implicit session => {
      "Clicks component" when {
        "no links created" should {
          "allow to create users" in {
            createUserTokens
          }
          "allow to create links" in {
            createLinksWithAutoCodeForUsers
          }
          "fail to create clicks with bad code" in {
            for (i <- 0 to 10) {
              val code: String = s"unknow${i}"
              val linkNotExists = the[LinkDoesNotExists] thrownBy {
                clicks.addClickForCode(code, new Timestamp(System.currentTimeMillis), "http://ya.ru", "127.0.0.1")
              }
              linkNotExists.code should be(code)
            }
          }
          "allow to create clicks with proper code" in {
            codeToLink.keys.zipWithIndex.foreach({
              case (code, index) =>
                val createdClicks = (0 to index).map(num => {
                  val timestamp = new Timestamp(System.currentTimeMillis)
                  val referer = s"http://ya.ru/page-${index}"
                  val remote_ip = s"127.0.0.${index}"
                  val clickForCode = clicks.addClickForCode(code, timestamp, referer, remote_ip)
                  clickForCode.linkCode should  be(code)
                  clickForCode.date should  be(timestamp)
                  clickForCode.referer should  be(referer)
                  clickForCode.remote_ip should  be(remote_ip)
                  clickForCode
                }).toList
                codeToClicks.update(code, createdClicks)
            })
          }
        }
      }
    }
  }
}
