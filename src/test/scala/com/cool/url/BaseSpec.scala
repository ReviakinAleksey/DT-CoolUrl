package com.cool.url

import com.cool.url.config.{Environment, DbConfig, ConfigProviderComponent, ProductionConfigProvider}
import com.cool.url.model.{ServiceComponent, SchemaComponent}
import com.cool.url.service.{QueryExtensions, SlickDbConnectorComponent, DbConnectorComponent, SaltedTokenComponent}
import org.scalatest.WordSpec

trait BaseSpec extends WordSpec {

  val context = new Environment("test") with SchemaComponent


}
