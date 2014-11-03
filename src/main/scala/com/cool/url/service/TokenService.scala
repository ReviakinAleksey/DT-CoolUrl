package com.cool.url.service

import java.nio.charset.Charset
import java.util.UUID

trait TokenServiceComponent {
  def tokenService: TokenService

  trait TokenService {
    def generate(id: Long): String
  }

}

trait SaltedTokenComponent extends TokenServiceComponent {

  class SaltedTokenService extends TokenService {
    private val TOKEN_CHARSET = Charset.forName("UTF-8")
    private val SALT = "NZ6#T<8.z/N@ewm"


    def generate(id: Long): String = UUID.nameUUIDFromBytes((SALT + id.toString).getBytes(TOKEN_CHARSET)).toString
  }

}

