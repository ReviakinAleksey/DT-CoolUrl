package com.cool.url

import com.cool.url.config.{ConfigProviderComponent, ProductionConfigProvider}
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
    val config = new ProductionConfig
    val connector = new SlickDbConnector
    val tokenService = new SaltedTokenService
  }

}
