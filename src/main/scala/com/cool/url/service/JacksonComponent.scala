package com.cool.url.service

import com.fasterxml.jackson.databind.{DeserializationFeature, DeserializationConfig, SerializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper


trait JacksonComponent {

  val mapper = new ObjectMapper() with ScalaObjectMapper
  mapper.registerModule(DefaultScalaModule)
  mapper.enable(SerializationFeature.INDENT_OUTPUT)
  mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

}
