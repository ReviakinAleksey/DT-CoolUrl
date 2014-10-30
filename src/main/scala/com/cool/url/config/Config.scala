package com.cool.url.config

import java.io.File

import com.cool.url.service.JacksonComponent

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


trait JsonConfigProvider extends ConfigProviderComponent {
  self: JacksonComponent =>
  def readConfig(config: File): ConfigProvider = mapper.readValue[JsonConfigProvider.JsonConfig](config)

}

object JsonConfigProvider {
  private class JsonConfig(val db: JsonDbConfig, val httpPort: Int, val backendSecret: String) extends ConfigProvider
  private class JsonDbConfig(val host: String, val base: String, val user: String, val password: String, val schema: String) extends DbConfig

}
