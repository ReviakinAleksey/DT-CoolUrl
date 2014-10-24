package com.cool.url.service

sealed trait ValidationException extends Exception


case class UserTokenUnknown(token: String) extends ValidationException

case class FolderIsNotExists(folderId: Long) extends ValidationException

case class LinkCodeIsAlreadyOccupied(code: String) extends ValidationException