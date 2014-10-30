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
          "fail to create clikc with invalid referer" in {
            val referer = "dsdfsdsdff"
            val timestamp = new Timestamp(System.currentTimeMillis)
            val remoteIp = s"127.0.0.30"
            val code = codeToLink.keys.head
            val urlMalformed = the[UrlMalformed] thrownBy {
              clicks.addClickForCode(code, timestamp, referer, remoteIp)
            }
            urlMalformed.url should be(referer)
          }
          "allow to create clicks with proper code" in {
            codeToLink.keys.zipWithIndex.foreach({
              case (code, index) =>
                val createdClicks = (0 to index).map(num => {
                  val timestamp = new Timestamp(System.currentTimeMillis)
                  val referer = s"http://ya.ru/page-${index}"
                  val remoteIp = s"127.0.0.${index}"
                  val clickForCode = clicks.addClickForCode(code, timestamp, referer, remoteIp)
                  clickForCode.linkCode should  be(code)
                  clickForCode.date should  be(timestamp)
                  clickForCode.referer should  be(referer)
                  clickForCode.remoteIp should  be(remoteIp)
                  clickForCode
                }).toList
                codeToClicks.update(code, createdClicks)
            })
          }
        }
        "links created" should {
          "match previously created  clicks count with link binding" in {
            tokensToUserId.foreach({
              case (token, userId) =>
                userIdToLinks(userId).foreach(link => {
                  val (receivedLink, count) = links.linkAndClicksCountByCode(token, link.code)
                  receivedLink should be(link)
                  count should be(codeToClicks(link.code).length)
                })
            })
          }
        }
      }
    }
  }

  override protected def afterAll(): Unit = {
    connector.db.withSession(clearData(_))
    super.afterAll()
  }
}
