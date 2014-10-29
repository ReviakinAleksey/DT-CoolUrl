package com.cool.url.config

import java.io.File

import com.cool.url.service.JacksonComponent
import com.fasterxml.jackson.annotation.{JsonProperty, JsonCreator}

trait DbConfig {
  val host: String
  val base: String
  val user: String
  val password: String
  val schema: String
}

trait ConfigProvider {
  val db: DbConfig
  val httpPort: Int
  val backendSecret: String
}

trait ConfigProviderComponent {
  val config: ConfigProvider

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
    val httpPort = 8080
    val backendSecret = "67^]UarhSB-pVn8"
  }

}


trait JsonConfigProvider extends ConfigProviderComponent {
  self: JacksonComponent =>
  def readConfig(config: File): ConfigProvider = mapper.readValue[JsonConfigProvider.JsonConfig](config)

}

object JsonConfigProvider {
  private class JsonConfig(val db: JsonDbConfig, val httpPort: Int, val backendSecret: String) extends ConfigProvider
  private class JsonDbConfig(val host: String, val base: String, val user: String, val password: String, val schema: String) extends DbConfig

}
