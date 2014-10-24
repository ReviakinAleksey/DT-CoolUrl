package com.cool.url.config

import com.cool.url.service.{SlickDbConnectorComponent, DbConnectorComponent}

trait DbConfig {
  val host: String
  val base: String
  val user: String
  val password: String
  val schema: String
}

trait ConfigProviderComponent {
  val config: ConfigProvider

  trait ConfigProvider {
    val db: DbConfig
  }

}

trait ProductionConfigProvider extends ConfigProviderComponent {

  class ProductionConfig extends ConfigProvider {
    val db = new DbConfig {
      val host: String = "localhost:5432"
      val base: String = "url_t"
      val user: String = "uurl"
      val password: String = "uurl"
      val schema = "production"
    }
  }

}

object Config {
  lazy val PRODUCTION = new ConfigProviderComponent
    with ProductionConfigProvider
    with DbConnectorComponent
    with SlickDbConnectorComponent {
    val config = new ProductionConfig
    val connector = new SlickDbConnector
  }
}
