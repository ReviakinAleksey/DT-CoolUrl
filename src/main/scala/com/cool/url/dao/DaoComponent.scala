package com.cool.url.dao

import java.sql.Timestamp

import com.cool.url.config.ConfigProviderComponent
import com.cool.url.model.ServiceComponent
import com.cool.url.service._


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
  }

}


trait SlickDaoComponent extends DaoComponent {
  self: ConfigProviderComponent
    with DbConnectorComponent
    with SlickDbConnectorComponent
    with TokenServiceComponent
    with QueryExtensions
    with HardCodedAuthService
    with ServiceComponent =>


  class SlickDaoService extends DaoService {
    def authorizeAndGetToken(target: AuthObject, userId: Long) = if (externalAuthService.isAuthorized(target)) {
      connector.db.withSession({
        implicit session =>
          users.getToken(userId)
      })
    } else {
      throw new ExternalAuthFailure(target, ValidationException.INTERNAL_ERROR)
    }


    def createLink(token: UserToken, url: String, code: Option[LinkCode], folderId: Option[FolderId]): Link = connector.db.withSession({
      implicit session => {
        links.create(token, url, code, folderId)
      }
    })

    def getLink(code: LinkCode, referer: String, remoteIp: String): Link = connector.db.withSession({
      implicit session => {
        clicks.addClickForCode(code, new Timestamp(System.currentTimeMillis), referer, remoteIp)
        links.linkByCode(code)
      }
    })


    def getLinkAndCountOfClicks(token: UserToken, code: LinkCode): (Link, Int) = connector.db.withSession({
      implicit session => {
        links.linkAndClicksCountByCode(token, code)
      }
    })


    def readLinkByToken(token: UserToken, paging: Option[Paging]): PagingResult[Link] = connector.db.withSession({
      implicit session => {
        links.linksByToken(token, paging)
      }
    })


    def readClicksByTokenAndCode(token: UserToken, code: LinkCode, paging: Option[Paging]) = connector.db.withSession({
      implicit session => {
        links.linkByCodeAndToken(token, code)
        clicks.linksForCode(code, paging)
      }
    })

    def readLinksByFolder(token: UserToken, folderId: FolderId, paging: Option[Paging]) = connector.db.withSession({
      implicit session => {
        folders.folderByTokenAndId(token, folderId)
        links.linksByFolder(folderId, paging)
      }
    })

    def readFoldersByToken(token: UserToken, paging: Option[Paging]) = connector.db.withSession({
      implicit session => {
        folders.foldersByToken(token, paging)
      }
    })
  }

}

