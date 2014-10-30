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


  def createLinksWithAutoCodeForUsers(implicit session:Session): Iterable[Link] = {
    tokensToUserId.zipWithIndex.flatMap({ case ((token, userId), index) =>
      for {
        i <- 0 to index
        link = {
          val link = links.create(token, s"http://google.com/$index/${i + 1}", None, None)
          codeToLink += link.code -> link
          userIdToLinks(userId) += link
          link
        }
      } yield link
    })
  }

  override def clearData(implicit session: Session) = {
    tokensToUserId.keys.foreach(links.deleteByToken(_))
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
          "fail to create link with invalid url" in {
            val token = tokensToUserId.keys.headOption.get
            val url = "dsdfsdsdff"
            val urlMalformed = the[UrlMalformed] thrownBy {
              links.create(token, url, None, None)
            }
            urlMalformed.url should be(url)
          }
          "allow to create links with auto generated tokens" in {
            createLinksWithAutoCodeForUsers.foreach(_.code.length should be > 0)
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
            val code = codeToLink.keys.head
            val codeOccupied = the[LinkCodeIsAlreadyOccupied] thrownBy {
              links.create(token, "http://google.com", Some(code), None)
            }
            codeOccupied.code should be(code)
          }
          "return list of links by token" in {
            tokensToUserId.foreach({ case (token, userId) =>
              val userLinks: PagingResult[Link] = links.linksByToken(token)
              val checkedList: ListBuffer[Link] = userIdToLinks(userId)
              userLinks.count should be(checkedList.length)
              userLinks.data.length should be(checkedList.length)
              userLinks.data should contain only (checkedList: _*)
            })
          }
          "return link fow correct code and token" in {
            userIdToLinks.foreach({ case (userId, checkedLinks) =>
              checkedLinks.foreach(checkedLink => {
                links.linkByCodeAndToken(checkedLink.token, checkedLink.code) should be(checkedLink)
              })
            })
          }
          "fail to return link for token of different user" in {
            val token = tokensToUserId.keys.head
            val link = links.create(token, s"http://google.com/private", None, None)
            val otherUserToken = tokensToUserId.keys.filterNot(_ == token).head
            val linkDoesNotExists = the[LinkDoesNotExists] thrownBy links.linkByCodeAndToken(otherUserToken, link.code)
            linkDoesNotExists.code should be(link.code)
          }
          //TODO:  Generalize this
          "paginate by token" in {
            val (token, userId) = tokensToUserId.last
            val checkedList: ListBuffer[Link] = userIdToLinks(userId)
            val checkedLength: Int = checkedList.length
            for (limit <- 0 to checkedLength) {
              val paginated: PagingResult[Link] = links.linksByToken(token, Some(Paging(0, Some(limit))))
              paginated.count should be(checkedLength)
              val receivedLinks: List[Link] = paginated.data
              receivedLinks.length should be(limit)
              receivedLinks.foreach(checkedList.contains(_) should be(true))
            }
            for (offset <- 0 to checkedLength) {
              val paginated: PagingResult[Link] = links.linksByToken(token, Some(Paging(offset, Some(checkedLength))))
              paginated.count should be(checkedLength)
              val receivedLinks: List[Link] = paginated.data
              receivedLinks.length should be(checkedLength - offset)
              receivedLinks.foreach(checkedList.contains(_) should be(true))
            }
            val emptyPagination: PagingResult[Link] = links.linksByToken(token, Some(Paging(checkedLength)))
            emptyPagination.count should be(checkedLength)
            val receivedLinks: List[Link] = emptyPagination.data
            receivedLinks.length should be(0)
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
