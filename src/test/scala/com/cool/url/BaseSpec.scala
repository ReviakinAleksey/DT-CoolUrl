package com.cool.url

import com.cool.url.config.{DbConfig, ConfigProviderComponent, ProductionConfigProvider}
import com.cool.url.model.{ServiceComponent, SchemaComponent}
import com.cool.url.service.{QueryExtensions, SlickDbConnectorComponent, DbConnectorComponent, SaltedTokenComponent}
import org.scalatest.WordSpec

trait BaseSpec extends WordSpec {

  val context = new ConfigProviderComponent
    with ProductionConfigProvider
    with DbConnectorComponent
    with SlickDbConnectorComponent
    with SaltedTokenComponent
    with QueryExtensions
    with ServiceComponent
    with SchemaComponent {
    val config = new ProductionConfig {
      override val db = new DbConfig {
        val host: String = "localhost:5432"
        val base: String = "url_t"
        val user: String = "uurl"
        val password: String = "uurl"
        val schema = "test"
      }
    }
    val connector = new SlickDbConnector
    val tokenService = new SaltedTokenService
  }

}
