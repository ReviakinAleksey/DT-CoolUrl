package com.cool.url.service

import java.net.{InetAddress, URI, URL}

import com.google.common.net.{UrlEscapers, InetAddresses}

import scala.util.{Failure, Success, Try}

object ParametersValidation {

  type ValueContext[T] = Either[String, T]

  trait ValidValueTransformer[F, T] {

    def transform(target: F): ValueContext[T]

    def and[N](next: ValidValueTransformer[T, N]): ValidValueTransformer[F, N] = {
      val self = this
      new ValidValueTransformer[F, N] {
        def transform(target: F): ValueContext[N] = {
          self.transform(target).right.flatMap(next.transform(_))
        }
      }
    }
  }

  case class ParameterValidationFailed[T](parameter: T, path: String, reason: String)

  class ParameterValidator[F](parameter: F, path: String) {
    def ensure[T](transformer: ValidValueTransformer[F, T]): Either[ParameterValidationFailed[F], T] = {
      transformer.transform(parameter).left.map(ParameterValidationFailed(parameter, path, _))
    }
  }


  implicit class AsParameterValidator[T](x: T) {
    def validateAs(path: String): ParameterValidator[T] = new ParameterValidator(x, path)
  }

  implicit def asValidator[F, T](x: Either[ParameterValidationFailed[F], T]): Try[T] = {
    x match {
      case Right(result) => Success(result)
      case Left(failed) => Failure(ParameterInvalid(failed, ValidationException.INTERNAL_ERROR))
    }
  }

  implicit def asResult[T](x: Try[T]): T = x.get


  val beNonEmpty = new ValidValueTransformer[String, String] {
    def transform(target: String): ValueContext[String] =
      if (target.trim.length > 0) {
        Right(target)
      } else {
        Left("must.be.non.empty")
      }
  }

  val beTrimmed = new ValidValueTransformer[String, String] {
    def transform(target: String): ValueContext[String] =
      if (target.trim.length == target.length) {
        Right(target)
      } else {
        Left("must.be.trimmed")
      }
  }

  def haLengthLE(length: Int) = new ValidValueTransformer[String, String] {
    def transform(target: String): ValueContext[String] =
      if (target.length <= length) {
        Right(target)
      } else {
        Left("must.be.non.empty")
      }
  }

  val intValue = new ValidValueTransformer[String, Int] {
    def transform(target: String): ValueContext[Int] = {
      try {
        Right(target.toInt)
      } catch {
        case _: NumberFormatException =>
          Left("must.be.int")
      }
    }
  }


  val beURL = new ValidValueTransformer[String, URI] {
    def transform(target: String): ValueContext[URI] = {
      try {
        Right(new URL(target).toURI)
      } catch {
        case _: Throwable => Left("must.be.url")
      }
    }
  }

  val beIP = new ValidValueTransformer[String, InetAddress] {
    def transform(target: String): ValueContext[InetAddress] = {
      try {
        Right(InetAddresses.forString(target))
      } catch {
        case _: Throwable => Left("must.be.url")
      }
    }
  }

  val beValidPath = new ValidValueTransformer[String, String] {
    def transform(target: String): ValueContext[String] = {
      if (UrlEscapers.urlPathSegmentEscaper().escape(target) == target) {
        Right(target)
      } else {
        Left("must.be.valid.url.path")
      }
    }
  }
}
