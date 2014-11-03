package com.cool.url.config

import java.io.File

import com.cool.url.service.JacksonComponent

trait DbConfig {
  def host: String
  def base: String
  def user: String
  def password: String
  def schema: String
}

trait ConfigProvider {
  def db: DbConfig
  def httpPort: Int
  def backendSecret: String
}

trait ConfigProviderComponent {
  def config: ConfigProvider

}


trait JsonConfigProvider extends ConfigProviderComponent {
  self: JacksonComponent =>
  def readConfig(config: File): ConfigProvider = mapper.readValue[JsonConfigProvider.JsonConfig](config)

}

object JsonConfigProvider {
  private class JsonConfig(val db: JsonDbConfig, val httpPort: Int, val backendSecret: String) extends ConfigProvider
  private class JsonDbConfig(val host: String, val base: String, val user: String, val password: String, val schema: String) extends DbConfig

}
