package com.cool.url.service

import com.cool.url.config.ConfigProviderComponent

trait ExternalAuthComponent {
  type AuthObject

  def externalAuthService: ExternalAuthService

  trait ExternalAuthService {
    def isAuthorized(target: AuthObject): Boolean
  }

}

trait HardCodedAuthService extends ExternalAuthComponent {

  self: ConfigProviderComponent =>

  type AuthObject = String

  class HardCodedAuthService extends ExternalAuthService {
    def isAuthorized(target: String): Boolean = target == config.backendSecret
  }

}



