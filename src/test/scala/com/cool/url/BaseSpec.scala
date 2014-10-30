package com.cool.url

import com.cool.url.config.Environment
import com.cool.url.model.SchemaComponent
import org.scalatest.WordSpec

trait BaseSpec extends WordSpec {

  val context = new Environment("test") with SchemaComponent


}
