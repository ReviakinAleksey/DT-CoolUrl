package com.cool.url

import java.sql.Connection
import com.cool.url.service._
import org.scalatest.{Suite, BeforeAndAfterAll}
import scala.collection.immutable.IndexedSeq




class MainSpec extends BaseSpec with BeforeAndAfterAll {
  override val invokeBeforeAllAndAfterAllEvenIfNoTestsAreExpected = true

  override protected def beforeAll(): Unit = {

    import context._
    import context.connector.driver.simple._

    connector.schema.map(schemaName => {
      val connection: Connection = connector.db.createConnection()
      try {
        val stm = connection.createStatement()
        stm.execute(s"DROP SCHEMA IF EXISTS ${schemaName} CASCADE")
        stm.execute(s"CREATE SCHEMA ${schemaName}")
        stm.close()
      } finally {
        connection.close()
      }
    })
    ddl.createStatements.foreach(println)
    connector.db.withSession {
      implicit session => {
        ddl.create
      }
    }
    super.beforeAll()
  }

  override protected def afterAll(): Unit = {
    import context.connector.driver.simple._
    import context._
    ddl.dropStatements.foreach(println)
    connector.db.withSession {
      implicit session => {
        ddl.drop
      }
    }
    try {
      connector.shutdown
    } finally {
    }
    super.afterAll()
  }

  override def nestedSuites: IndexedSeq[Suite] = Vector(new TokenSpec,
    new UsersComponentSpec,
    new LinksComponentSpec,
    new ClicksComponentSpec)
}
