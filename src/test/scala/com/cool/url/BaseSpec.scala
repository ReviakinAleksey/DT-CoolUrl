package com.cool.url

import com.cool.url.config.Environment
import com.cool.url.model.SchemaComponent
import com.cool.url.service.ParameterInvalid
import org.scalatest.WordSpec
import org.scalatest.Matchers._

trait BaseSpec extends WordSpec {

  val context = new Environment("test") with SchemaComponent

  implicit class CheckParameterInvalid[T](x: T) {
    def mustBeReportedWith(f:  => Unit)(implicit manifest:Manifest[ParameterInvalid[(String, String, T)]]) {
      val parameterInvalid = the[ParameterInvalid[(String, String, T)]] thrownBy {
        f
      }
      parameterInvalid.parameters._3 should be(x)
    }
  }


}
