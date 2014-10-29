package com.cool.url.service

import io.netty.handler.codec.http.HttpResponseStatus


case class ValidationResponse[T]( messageCode: String,parameters: T )

sealed trait ValidationException[T] extends Exception {
  val status: ValidationException.Status
  val messageCode: String
  val parameters: T

  def httpContent = ValidationResponse[T](messageCode, parameters)
}


object ValidationException {
  type Status = HttpResponseStatus

  val NOT_FOUND = HttpResponseStatus.NOT_FOUND
  val INTERNAL_ERROR = HttpResponseStatus.INTERNAL_SERVER_ERROR
}

case class ExternalAuthFailure(secret: String, status: ValidationException.Status) extends ValidationException[List[String]] {
  val messageCode = "external.auth.code.unknown"
  val parameters = List(secret)
}

case class UserTokenUnknown(token: String, status: ValidationException.Status) extends ValidationException[List[String]] {
  val messageCode = "user.token.unknown"
  val parameters = List(token)
}

case class FolderIsNotExists(folderId: Long, status: ValidationException.Status) extends ValidationException[List[Long]] {
  val messageCode = "folder.does.not.exists"
  val parameters = List(folderId)
}

case class LinkCodeIsAlreadyOccupied(code: String, status: ValidationException.Status) extends ValidationException[List[String]] {
  val messageCode = "link.code.already.occupied"
  val parameters = List(code)
}

case class LinkDoesNotExists(code: String, status: ValidationException.Status) extends ValidationException[List[String]] {
  val messageCode = "link.does.not.exist"
  val parameters = List(code)
}