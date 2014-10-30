package com.cool.url.web.dto

case class TokenResponse(token: String)

case class LinkResponse(url: String)

case class LinkAndClicksResponse[L](link: L, clicks: Int)
