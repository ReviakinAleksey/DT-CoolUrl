package com.cool.url.dao

import java.sql.Timestamp

import com.cool.url.config.ConfigProviderComponent
import com.cool.url.model.ServiceComponent
import com.cool.url.service._
import com.typesafe.scalalogging.StrictLogging


trait DaoComponent {
  self: ExternalAuthComponent
    with ServiceComponent =>
  val daoService: DaoService

  trait DaoService {
    def authorizeAndGetToken(target: AuthObject, userId: Long): UserToken

    def createLink(token: UserToken, url: String, code: Option[LinkCode], folderId: Option[FolderId]): Link

    def getLink(code: LinkCode, referer: String, remoteIp: String): Link

    def getLinkAndCountOfClicks(token: UserToken, code: LinkCode): (Link, Int)

    def readLinkByToken(token: UserToken, paging: Option[Paging]): PagingResult[Link]

    def readClicksByTokenAndCode(token: UserToken, code: LinkCode, paging: Option[Paging]): PagingResult[Click]

    def readLinksByFolder(token: UserToken, folderId: FolderId, paging: Option[Paging]): PagingResult[Link]

    def readFoldersByToken(token: UserToken, paging: Option[Paging]): PagingResult[Folder]

    def createFolder(token: UserToken, title: String): Folder
  }

}


trait SlickDaoComponent extends DaoComponent with StrictLogging {
  self: ConfigProviderComponent
    with DbConnectorComponent
    with SlickDbConnectorComponent
    with TokenServiceComponent
    with QueryExtensions
    with HardCodedAuthService
    with ServiceComponent =>


  class SlickDaoService extends DaoService {
    def authorizeAndGetToken(target: AuthObject, userId: Long):UserToken = if (externalAuthService.isAuthorized(target)) {
      logger.debug(s"User with userId: $userId requested for a token")
      connector.db.withSession({
        implicit session =>
          val token = users.getToken(userId)
          logger.debug(s"User with userId: $userId registered at token: $token")
          token
      })
    } else {
      logger.warn(s"User with userId: $userId requested for a token with a fraud secret: $target")
      throw new ExternalAuthFailure(target, ValidationException.FORBIDDEN)
    }


    def createLink(token: UserToken, url: String, code: Option[LinkCode], folderId: Option[FolderId]):Link = {
      logger.debug(s"User with token: $token requested to create link(url: $url, code: $code, folderId: $folderId)")
      connector.db.withSession({
        implicit session => {
          val link = links.create(token, url, code, folderId)
          logger.debug(s"User with token: $token registered new link $link")
          link
        }
      })
    }

    def getLink(code: LinkCode, referer: String, remoteIp: String): Link = {
      logger.debug(s"Click for link with code: $code registration start(referer: $referer, remoteIp: $remoteIp)")
      connector.db.withSession({
        implicit session => {
          val click = clicks.addClickForCode(code, new Timestamp(System.currentTimeMillis), referer, remoteIp)
          logger.debug(s"Click link with code: $code registered($click)")
          val link= links.linkByCode(code)
          logger.debug(s"Click for link with code: $code registration finished(returning link $link)")
          link
        }
      })
    }


    def getLinkAndCountOfClicks(token: UserToken, code: LinkCode): (Link, Int) = {
      logger.debug(s"User with token: $token requested to obtain link and count of clicks for link code: $code")
      connector.db.withSession({
        implicit session => {
          val (link, clicks) = links.linkAndClicksCountByCode(token, code)
          logger.debug(s"User with token: $token obtained link and count of clicks: ($link, $clicks)")
          (link, clicks)
        }
      })
    }


    def readLinkByToken(token: UserToken, paging: Option[Paging]): PagingResult[Link] = {
      logger.debug(s"User with token: $token requested to obtain links with paging: $paging")
      connector.db.withSession({
        implicit session => {
          val result = links.linksByToken(token, paging)
          logger.debug(s"User with token: $token obtained links with paging: $paging, links count is ${result.count}")
          result
        }
      })
    }


    def readClicksByTokenAndCode(token: UserToken, code: LinkCode, paging: Option[Paging]): PagingResult[Click] = {
      logger.debug(s"User with token: $token requested to read clicks for link code: $code with paging $paging")
      connector.db.withSession({
        implicit session => {
          val link = links.linkByCodeAndToken(token, code)
          logger.debug(s"User with token: $token requested to read clicks for link $link")
          val result = clicks.linksForCode(code, paging)
          logger.debug(s"User with token: $token obtained clicks for link $link, clicks count is ${result.count}")
          result
        }
      })
    }

    def readLinksByFolder(token: UserToken, folderId: FolderId, paging: Option[Paging]): PagingResult[Link] = {
      logger.debug(s"User with token: $token requested requested to read folder content of folder with id $folderId, with paging $paging")
      connector.db.withSession({
        implicit session => {
          val folder = folders.folderByTokenAndId(token, folderId)
          logger.debug(s"User with token: $token requested requested to read folder content of folder: $folder")
          val result = links.linksByFolder(folderId, paging)
          logger.debug(s"User with token: $token obtained folder content of folder: $folder, links count is ${result.count}")
          result
        }
      })
    }

    def readFoldersByToken(token: UserToken, paging: Option[Paging]): PagingResult[Folder]  = {
      logger.debug(s"User with token: $token requested to read folders, with paging $paging")
      connector.db.withSession({
        implicit session => {
          val result = folders.foldersByToken(token, paging)
          logger.debug(s"User with token: $token obtained folders, count is ${result.count}")
          result
        }
      })
    }

    def createFolder(token: UserToken, title: String): Folder = {
      logger.debug(s"User with token: $token requested to create folder with title: $title")
      connector.db.withSession({
        implicit session =>
          val result = folders.createForToken(token,title)
          logger.debug(s"User with token: $token created folder: $result")
          result
      })
    }
  }

}

