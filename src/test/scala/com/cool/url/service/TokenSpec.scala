package com.cool.url.service

import com.cool.url.BaseSpec
import org.scalatest.Matchers._


class TokenSpec extends BaseSpec {
  import context._
  "Token service" when {
    "initialized" should {
      "generate token" in {
        val token = tokenService.generate(123)
        token should have length 36
      }
      "should generate different tokens from different input" in {
        val token1 = tokenService.generate(125)
        val token2 = tokenService.generate(3250)
        token1 should not be token2
      }
      "should generate same tokens for same input" in {
        val token1 = tokenService.generate(3250)
        val token2 = tokenService.generate(3250)
        token1 shouldEqual token2
      }
    }
  }
}
