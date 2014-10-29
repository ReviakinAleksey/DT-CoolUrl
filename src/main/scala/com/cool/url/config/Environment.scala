package com.cool.url.config

import java.io.File

import com.cool.url.dao.SlickDaoComponent
import com.cool.url.model.ServiceComponent
import com.cool.url.service._
import com.cool.url.web.UnfilteredWebComponent


class Environment(envName: String) extends JsonConfigProvider
  with DbConnectorComponent
  with SlickDbConnectorComponent
  with SaltedTokenComponent
  with QueryExtensions
  with ServiceComponent
  with HardCodedAuthService
  with SlickDaoComponent
  with UnfilteredWebComponent
  with JacksonComponent {


  val config = readConfig(new File(s"$envName-config.json"))
  val webService = new UnfilteredWebService
  val daoService = new SlickDaoService
  val connector = new SlickDbConnector
  val tokenService = new SaltedTokenService
  val externalAuthService = new HardCodedAuthService

}
