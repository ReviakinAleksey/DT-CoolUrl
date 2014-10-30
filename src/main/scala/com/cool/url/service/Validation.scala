package com.cool.url.service

import java.net.URL

import io.netty.handler.codec.http.HttpResponseStatus
import scala.util.{Failure, Success, Try}


case class ValidationResponse[T]( messageCode: String,parameters: T )

sealed trait ValidationException[T] extends Exception {
  val status: ValidationException.Status
  val messageCode: String
  val parameters: T

  def httpContent:ValidationResponse[T] = ValidationResponse[T](messageCode, parameters)
}


object ValidationException {
  type Status = HttpResponseStatus

  val NOT_FOUND = HttpResponseStatus.NOT_FOUND
  val FORBIDDEN = HttpResponseStatus.FORBIDDEN
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

case class FolderAlreadyExists(title: String, status: ValidationException.Status) extends ValidationException[List[String]] {
  val messageCode = "folder.already.exist"
  val parameters = List(title)
}


case class UrlMalformed(url: String, path: String, status: ValidationException.Status) extends ValidationException[List[String]] {
  val messageCode = "provided.url.malformed"
  val parameters = List(url,path)
}

object UrlMalformed {
  def validate(url: String, path: String, status: ValidationException.Status): Unit = {
    Try(new URL(url).toURI).transform(Success(_), _ => Failure(UrlMalformed(url, path, status))).get
  }
}
