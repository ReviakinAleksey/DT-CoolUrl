package com.cool.url.web

import java.io.OutputStream
import java.net.URI
import java.util.concurrent.Executors

import com.cool.url.config.ConfigProviderComponent
import com.cool.url.dao.DaoComponent
import com.cool.url.service._
import com.cool.url.web.dto.{LinkAndClicksResponse, LinkResponse, TokenResponse}
import com.typesafe.scalalogging.StrictLogging
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.{ChannelFutureListener, ChannelHandlerContext, ChannelInboundHandler}
import io.netty.handler.codec.http.{DefaultFullHttpResponse, HttpResponseStatus, HttpVersion}
import unfiltered.directives.Directives._
import unfiltered.directives.Result.{Failure, Success}
import unfiltered.directives._
import unfiltered.netty.ExceptionHandler
import unfiltered.netty.cycle.{DeferredIntent, DeferralExecutor, Plan, ThreadPool}
import unfiltered.request._
import unfiltered.response._
import unfiltered.util.RunnableServer
import unfiltered.util.control.NonFatal

import scala.io.Source


trait WebComponent {
  val webService: WebService

  trait WebService {
    def start(): Unit

    def stop(): Unit
  }

}


trait WebPlan extends StrictLogging {
  self: JacksonComponent =>

  object WebPlan {

    type ExceptionResponseBuilder = () => DefaultFullHttpResponse

    @Sharable
    trait CustomServerErrorResponse extends ExceptionHandler {
      self: ChannelInboundHandler =>


      def onException(ctx: ChannelHandlerContext, t: Throwable) = {

        val responseBuilder: ExceptionResponseBuilder = t match {
          case ve: ValidationException[_] =>
            () => new DefaultFullHttpResponse(
              HttpVersion.HTTP_1_1, ve.status,
              Unpooled.copiedBuffer(mapper.writeValueAsBytes(ve.httpContent)))
          case other =>
            logger.error("Web exception", t)
            () => new DefaultFullHttpResponse(
              HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR, Unpooled.copiedBuffer(
                mapper.writeValueAsBytes(ValidationResponse("internal.server.error", Nil))))
        }

        val ch = ctx.channel
        if (ch.isOpen) try {
          val response = responseBuilder()
          response.headers.add("Content-type", "application/json")
          ch.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE)
        } catch {
          case NonFatal(_) => ch.close()
        }
      }
    }
    private val executor = Executors.newCachedThreadPool()

    trait WebThreadPool extends DeferralExecutor with DeferredIntent {
      val underlying = executor
    }

    @Sharable
    class WebPlanned(intentIn: Plan.Intent) extends Plan with WebThreadPool with CustomServerErrorResponse {
      val intent = intentIn
    }


    def apply(intentIn: Plan.Intent) = new WebPlanned(intentIn)
  }


  case class ResponseJsonContent[T](content: T) extends ResponseStreamer {
    def stream(os: OutputStream) {
      os.write(mapper.writeValueAsBytes(content))
    }
  }

  def standardResponse[T](content: T) = JsContent ~> ResponseJsonContent(content)

}


trait UnfilteredWebComponent extends WebComponent with WebPlan {

  self: ConfigProviderComponent
    with DaoComponent
    with HardCodedAuthService
    with JacksonComponent =>

  class UnfilteredWebService extends WebService with StrictLogging {

    private var server: Option[RunnableServer] = None


    implicit val paramAsLong = data.as.String ~> data.as.Long.fail {
      (name, value) => JsContent ~> BadRequest ~> ResponseJsonContent(ValidationResponse("parameter type expected to by Long", List(name, value)))
    }

    implicit val paramAsInt = data.as.String ~> data.as.Int.fail {
      (name, value) => JsContent ~> BadRequest ~> ResponseJsonContent(ValidationResponse("parameter type expected to by Int", List(name, value)))
    }

    implicit def required[T] = data.Requiring[T].fail(name => {
      JsContent ~> BadRequest ~> ResponseJsonContent(ValidationResponse("parameter is missing", List(name)))
    })

    val refererExtractor = {
      new Directive[Any, AnyRef with ResponseFunction[Any], URI]((request => {
        Referer(request) match {
          case Some(value) => Success(value)
          case None =>
            Failure(JsContent ~> BadRequest ~> ResponseJsonContent(ValidationResponse("header is missing", List(Referer.name))))
        }
      }))
    }

    val pagingExtractor = (data.as.Option[Int] named "offset").flatMap(offsetOption => {
      (data.as.Option[Int] named "limit").map(limitOption => {
        (offsetOption, limitOption) match {
          case (None, None) => None
          case _ => Some(Paging(offsetOption.getOrElse(0), limitOption))
        }
      })
    })

    val tokenExtractor = data.as.Required[String] named "token"

    val classLoader = getClass.getClassLoader

    val basePlan = WebPlan {
      case Path(Seg("index.html"::Nil)) =>
        val content:String = Source.fromFile(classLoader.getResource("web/index.html").getFile).mkString
        HtmlContent ~> ResponseString(content)
      case Path(Seg("shutdown" :: Nil)) =>
        stop()
        ResponseString("ok")
    }

    val tokenPlan = WebPlan {
      {
        Directive.Intent {
          case Path(Seg("token" :: Nil)) =>
            for {
              secret <- data.as.Required[String] named "secret"
              userId <- data.as.Required[Long] named "userId"
            } yield {
              standardResponse(TokenResponse(daoService.authorizeAndGetToken(secret, userId)))
            }
        }
      }
    }

    object IntPath {
      def unapply(value: String): Option[Int] = {
        try {
          Some(value.toInt)
        } catch {
          case _: NumberFormatException => None
        }
      }
    }

    val linkPlan = WebPlan {
      {
        Directive.Intent {
          case req@Path(Seg("link" :: _)) =>
            req match {
              case POST(Path(Seg(_ :: Nil))) =>
                for {
                  token <- tokenExtractor
                  url <- data.as.Required[String] named "url"
                  code <- data.as.Option[String] named "code"
                  folder <- data.as.Option[Long] named "folder_id"
                } yield {
                  standardResponse(daoService.createLink(token, url, code, folder))
                }
              case POST(Path(Seg(_ :: code :: Nil))) =>
                for {
                  referer <-  data.as.Required[String] named "referer"
                  remoteIp <-  data.as.Required[String] named "remote_ip"
                } yield {
                  standardResponse(LinkResponse(daoService.getLink(code, referer, remoteIp).url))
                }
              case GET(Path(Seg(_ :: code :: Nil))) =>
                for {
                  token <- tokenExtractor
                } yield {
                  val (link, clicks) = daoService.getLinkAndCountOfClicks(token, code)
                  standardResponse(LinkAndClicksResponse(link, clicks))
                }
              case GET(Path(Seg(_ :: Nil))) =>
                for {
                  token <- tokenExtractor
                  paging <- pagingExtractor
                } yield {
                  standardResponse(daoService.readLinkByToken(token, paging))
                }
              case GET(Path(Seg(_ :: code :: "clicks" :: Nil))) =>
                for {
                  token <- tokenExtractor
                  paging <- pagingExtractor
                } yield {
                  standardResponse(daoService.readClicksByTokenAndCode(token, code, paging))
                }
            }
        }
      }
    }

    val folderPlan = WebPlan {
      Directive.Intent {
        case req@Path(Seg("folder" :: _)) =>
          req match {
            case POST(Path(Seg(_ :: Nil))) =>
              for {
                token <- tokenExtractor
                title <-  data.as.Required[String] named "title"
              } yield  {
                standardResponse(daoService.createFolder(token, title))
              }
            case GET(Path(Seg(_ :: IntPath(id) :: Nil))) =>
              for {
                token <- tokenExtractor
                paging <- pagingExtractor
              } yield {
                standardResponse(daoService.readLinksByFolder(token, id, paging))
              }
            case GET(Path(Seg(_ :: Nil))) =>
              for {
                token <- tokenExtractor
                paging <- pagingExtractor
              } yield {
                standardResponse(daoService.readFoldersByToken(token, paging))
              }
          }
      }
    }


    def start(): Unit = {

      server = Some(unfiltered.netty.Server.local(config.httpPort).
        plan(basePlan).plan(tokenPlan).plan(linkPlan).plan(folderPlan)
      )
      server.map(_.run())
    }

    def stop(): Unit = {
      server.map(_.stop())
    }
  }


}
