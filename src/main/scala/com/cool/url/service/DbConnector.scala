package com.cool.url.service

import com.cool.url.config.ConfigProviderComponent
import com.mchange.v2.c3p0.ComboPooledDataSource
import org.postgresql.util.PSQLException

import scala.slick.driver.{JdbcProfile, PostgresDriver}
import scala.slick.jdbc.JdbcBackend.Database
import scala.slick.lifted.AbstractTable


trait DbConnectorComponent {
  val connector: DbConnector

  trait DbConnector {
    def db: Database
    val driver: JdbcProfile
    def UNIQUE_VIOLATION: ConstraintViolation
    def schema: Option[String]

    def shutdown(): Unit
  }

  trait ConstraintViolation {

    def unapply(th: Throwable): Option[String]
  }

}

trait SlickDbConnectorComponent {
  this: DbConnectorComponent with ConfigProviderComponent =>

  class SlickDbConnector extends DbConnector {
    private val UNIQUE_VIOLATION_CODE = "23505"
    private val FOREIGN_KEY_VIOLATION = "23503"

    private val dataSource = {
      val source: ComboPooledDataSource = new ComboPooledDataSource()
      source.setDriverClass("org.postgresql.Driver")
      source.setJdbcUrl(s"jdbc:postgresql://${config.db.host}/${config.db.base}")
      source.setUser(config.db.user)
      source.setPassword(config.db.password)
      source
    }

    val driver = PostgresDriver

    val db: Database = Database.forDataSource(dataSource)


    val UNIQUE_VIOLATION = new ConstraintViolation {

      override def unapply(th: Throwable): Option[String] = th match {
        case psql: PSQLException if psql.getServerErrorMessage.getSQLState == UNIQUE_VIOLATION_CODE ||
          psql.getServerErrorMessage.getSQLState == FOREIGN_KEY_VIOLATION =>
            Some(psql.getServerErrorMessage.getConstraint)
        case _ =>
          None
      }
    }

    val schema = Some(config.db.schema)


    def shutdown(): Unit = {
      dataSource.close()
    }
  }

}


case class Paging(offset: Int, limit: Option[Int] = None)

case class PagingResult[E](count: Int, data: List[E])

trait QueryExtensions {
  this: DbConnectorComponent =>

  import connector.driver.simple._


  trait PaginationExtension[E <: AbstractTable[_]] {
    self:TableQuery[E] =>

    private val DEFAULT_PAGING_LIMIT = 50

    implicit class QueryExtensions[Q <: Query[E, E#TableElementType, Seq]](val q: Q){

      def paginate(paging: Paging): Query[E, E#TableElementType, scala.Seq] =
        q.drop(paging.offset).take(paging.limit.getOrElse(DEFAULT_PAGING_LIMIT))

      def rowsCount:Column[Int] = q.length

      def withPaging(pagingOption: Option[Paging])(implicit session: Session):PagingResult[E#TableElementType]  = {
        val query = pagingOption.map(paginate(_)).getOrElse(q)
        new PagingResult[E#TableElementType](q.rowsCount.run, query.list)
      }
    }
  }

}


