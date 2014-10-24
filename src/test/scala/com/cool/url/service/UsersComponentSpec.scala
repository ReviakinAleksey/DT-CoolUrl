package com.cool.url.service

import com.cool.url.BaseSpec

import org.scalatest.BeforeAndAfterAll
import org.scalatest.Matchers._

import scala.util.Random

trait UsersComponentFixture {
  self: BaseSpec =>

  import context._
  import context.connector.driver.simple._

  val userIdToTokens: collection.mutable.Map[Int, Option[UserToken]] = collection.mutable.Map(123121 -> None, 3423423 -> None, 523542 -> None)
  val tokensToUserId: collection.mutable.Map[UserToken, Int] = collection.mutable.Map.empty[UserToken, Int]

  def createUserTokens(implicit session: Session): List[String] = userIdToTokens.keys.map(id => {
    val token = users.getToken(id)
    userIdToTokens += id -> Some(token)
    tokensToUserId += token -> id
    token
  }).toList

  def clearData(implicit session: Session) = {
    userIdToTokens.keys.foreach(userId => users.filter(_.id === userId.toLong).delete)
  }

}


class UsersComponentSpec extends BaseSpec with UsersComponentFixture with BeforeAndAfterAll {

  import context._


  connector.db.withSession {
    implicit session => {
      "User component" when {
        "users empty" should {
          "return none for any token" in {
            for (id <- userIdToTokens.keys) {
              val token = tokenService.generate(Random.nextLong())
              users.getByToken(token) should equal(None)
            }
          }
          "allow to create tokens" in {
            for (id <- userIdToTokens.keys) {
              val token = users.getToken(id)
              token.length should be > 0
              userIdToTokens += id -> Some(token)
            }
          }
        }
        "user generated" should {
          "return same tokens for previously defined users" in {
            for ((id, generatedToken) <- userIdToTokens) {
              val token = users.getToken(id)
              generatedToken should be(Some(token))
            }
          }
          "able to find users by tokens" in {
            for ((id, Some(generatedToken)) <- userIdToTokens) {
              val user: Option[User] = users.getByToken(generatedToken)
              user should be(Some(User(id, generatedToken)))
            }
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
