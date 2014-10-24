package com.cool.url.service

import com.cool.url.BaseSpec
import org.scalatest.BeforeAndAfterAll
import org.scalatest.Matchers._

import scala.collection.mutable.ListBuffer

trait LinksComponentFixture extends UsersComponentFixture {
  self: BaseSpec =>

  import context._
  import context.connector.driver.simple._

  val userIdToLinks: collection.mutable.Map[Int, ListBuffer[Link]] = userIdToTokens.map({ case (id, _) => id -> ListBuffer.empty[Link]})
  val codeToLink = collection.mutable.Map.empty[LinkCode, Link]


  override def clearData(implicit session: Session) = {
    tokensToUserId.keys.foreach(links.deleteAllByToken(_)(session))
    super.clearData(session)
  }

}

class LinksComponentSpec extends BaseSpec with LinksComponentFixture with BeforeAndAfterAll {

  import context._

  connector.db.withSession {
    implicit session => {
      "links component" when {
        "users is not created" should {
          "fail to create link with incorrect token" in {
            val token: String = "61681ebe-6aba-3be6-8faa-d7911dbbd080"
            val tokenUnknown = the[UserTokenUnknown] thrownBy {
              links.create(token, "http://google.com", None, None)
            }
            tokenUnknown.token should be(token)
          }
          "create some users" in {
            createUserTokens.length should be > 0
          }
        }
        "user is present" should {
          "fail to create link with invalid folder" in {
            val folderId = 10
            val token = tokensToUserId.keys.headOption.get
            val folderIsNotExists = the[FolderIsNotExists] thrownBy {
              links.create(token, "http://google.com", None, Some(folderId))
            }
            folderIsNotExists.folderId should be(folderId)
          }
          "allow to create links with auto generated tokens" in {
            tokensToUserId.zipWithIndex.foreach({ case ((token, userId), index) =>
              for (i <- 0 to index) {
                val link = links.create(token, s"http://google.com/$index/${i + 1}", None, None)
                codeToLink += link.code -> link
                userIdToLinks(userId) += link
                link.code.length should be > 0
              }
            })
          }
          "allow to create links with predefined code" in {
            tokensToUserId.zipWithIndex.foreach({ case ((token, userId), index) =>
              for (i <- 0 to index) {
                val code = s"link-$index-$i"
                val link = links.create(token, s"http://google.com/predefined/$index/${i + 1}", Some(code), None)
                codeToLink += link.code -> link
                userIdToLinks(userId) += link
                link.code should be(code)
              }
            })
          }
          "fail to create link with previously used code" in {
            val token = tokensToUserId.keys.headOption.get
            val code  = codeToLink.keys.head
            val codeOccupied = the[LinkCodeIsAlreadyOccupied] thrownBy {
              links.create(token, "http://google.com", Some(code), None)
            }
            codeOccupied.code should be(code)
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
