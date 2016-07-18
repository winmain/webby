package webby.core.server.netty

import java.io.IOException
import javax.annotation.Nullable

import com.google.common.net.HttpHeaders._
import io.netty.buffer.Unpooled
import io.netty.channel.{ChannelFuture, ChannelFutureListener, ChannelHandlerContext, SimpleChannelInboundHandler}
import io.netty.handler.codec.http.HttpResponseStatus._
import io.netty.handler.codec.http._
import io.netty.util.CharsetUtil
import org.apache.commons.lang3.StringUtils
import webby.api.mvc.Results._
import webby.api.mvc._
import webby.api.{Application, Logger}
import webby.commons.io.cookie.CookieEncoderV0
import webby.commons.system.log.{LogWriterHolder, PageLog, PageLogWriterPlugin}
import webby.core.server.Server

import scala.util.{Failure, Success}


private[server] class UpstreamHandler(server: Server)
  extends SimpleChannelInboundHandler[FullHttpRequest] {

  // TODO: здесь возможны баги с этим контекстом. Например, когда запрос ушёл в async,
  // TODO: и после этого сервер начал процедуру завершения.
  // TODO: Сервер при завершении не учитывает этот контекст, и не дожидается его.
  implicit val internalExecutionContext = webby.core.Execution.internalContext

  private val requestIDs = new java.util.concurrent.atomic.AtomicLong(0)
  private val logger = Logger("netty")
  private val requestLogWriter = new LogWriterHolder[PageLogWriterPlugin] {
    override def onDisabledPlugin: PageLogWriterPlugin = null
  }

  override def isSharable: Boolean = true

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
    cause match {
      case e: IOException => // do nothing. Usually it's "Connection reset by peer"
      case e =>
        logger.warn("Exception caught in Netty", e)
    }
    if (ctx.channel().isActive) ctx.channel().close()
  }

  override def channelRead0(ctx: ChannelHandlerContext, req: FullHttpRequest) {
    if (!req.decoderResult().isSuccess) {
      sendError(ctx, BAD_REQUEST)
      return
    }

    logger.trace("Http request received by netty: " + req)
    val host: String = req.headers().get(HttpHeaderNames.HOST)
    if (host == null) {
      sendError(ctx, BAD_REQUEST)
      return
    }
    Thread.currentThread().setName("netty@" + Thread.currentThread().getId + ": " +
      req.method.name() + " " + host + req.uri)
    val ip: String = req.headers().get(X_FORWARDED_FOR)

    PageLog.set(new PageLog(ip = ip, host = host, method = req.method.name(), path = req.uri))

    try {
      val keepAlive = HttpUtil.isKeepAlive(req)
      //    val websocketableRequest = websocketable(msg)
      val nettyVersion = req.protocolVersion
      val reqHeaders = new Headers(req.headers())
      //    val rCookies = getCookies(req)

      class NettyRequestHeader(queryParams: UrlEncoded, uriPath: String) extends BaseRequestHeader {
        override val requestId = requestIDs.incrementAndGet
        override def uri = req.uri
        override def path = uriPath
        override def method = req.method
        override def version = nettyVersion.text()
        override def query = queryParams
        override def headers = reqHeaders
        //          lazy val remoteAddress = req.headers().get(X_FORWARDED_FOR)
        override def remoteAddress: String = ip
      }

      // Разобрать GET параметры запроса.
      // В случае ошибки разбора возвращаем результат Global.onBadRequest
      var requestHeader: RequestHeader = null
      val handlerOrResult: Either[Result, (Handler, Application)] =
        try {
          val uri: String = req.uri
          var path: String = null
          var query: String = null
          val urlEncoded: UrlEncoded =
            uri.indexOf('?') match {
              case -1 =>
                path = uri
                EmptyUrlEncoded

              case idx =>
                path = uri.substring(0, idx)
                query = uri.substring(idx + 1)
                UrlEncoded.fromQuery(query)
            }
          requestHeader = new NettyRequestHeader(urlEncoded, path)
          server.getHandlerFor(requestHeader)
        } catch {
          case e: Throwable =>
            requestHeader = new NettyRequestHeader(EmptyUrlEncoded, StringUtils.substringBefore(req.uri, "?"))
            server.applicationProvider.get match {
              case Left(_) => Left(BadRequest("Bad request: " + e.getMessage))
              case Right(a) => Left(a.global.onBadRequest(requestHeader, e.getMessage))
            }
        }

      // attach the cleanup function to the channel context for after cleaning
      //    ctx.setAttachment(cleanup _)

      // Обработка запроса и получение результата (выполнение Action)
      val result: Result = handlerOrResult match {
        //execute normal action
        case Right((action: Action, app)) =>
          // Read input bytes, handle action, and return result
          logger.trace("Serving this request with: " + action)

          val content = req.content()
          val readableBytes: Int = content.readableBytes()
          val bytes: Array[Byte] =
            if (readableBytes > 0) {
              val bytes = new Array[Byte](readableBytes)
              content.readBytes(bytes)
              bytes
            } else Array.emptyByteArray
          app.handleAction(action, requestHeader, bytes)

        case Left(e) =>
          logger.trace("No handler, got direct result: " + e)
          e

        case Right((action, app)) =>
          logger.error("Invalid action: " + action)
          Results.InternalServerError("Invalid action type")
      }

      // Вернуть результат
      def handleResult(result: Result, pageLog: PageLog) {
        result match {
          case r: PlainResult =>
            pageLog.setResultStatus(r.status.code())
            val resp = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, r.status)
            // Set response cookies
            for (cookie <- r.cookies)
              resp.headers().add(SET_COOKIE, CookieEncoderV0.encode(cookie))

            // Set response headers
            for ((name, value) <- r.headers)
              resp.headers().add(name, value)

            // Response header Connection: Keep-Alive is needed for HTTP 1.0
            if (keepAlive && nettyVersion == HttpVersion.HTTP_1_0)
              resp.headers().set(CONNECTION, "keep-alive")

            resp.headers().set(CONTENT_LENGTH, r.body.length)

            resp.content().writeBytes(r.body)
            val writeFuture: ChannelFuture = ctx.writeAndFlush(resp)
            if (!keepAlive) {
              writeFuture.addListener(ChannelFutureListener.CLOSE)
            }

          //        case AsyncResult(p) => p.extend1 {
          //          case Redeemed(v) => handle(v)
          //          case Thrown(e) =>
          //            server.applicationProvider.get match {
          //              case Right(app) => handle(app.handleError(requestHeader, e))
          //              case Left(_) => handle(Results.InternalServerError)
          //            }
          //        }

          case ar: AsyncResult =>
            val pageLog = PageLog.get()
            ar.execute(pageLog).onComplete {tryResult=>
              try {
                tryResult match {
                  case Success(r) =>
                    handleResult(r, pageLog)
                  case Failure(t) =>
                    server.applicationProvider.get match {
                      case Right(app) =>
                        logger.error("Error executing async action " + requestHeader.method.name() + " " +
                          requestHeader.domain + requestHeader.uri, t)
                        handleResult(app.global.onError(requestHeader, t, None), pageLog)
                      case Left(_) =>
                        logger.error("Error executing async action", t)
                        sendError(ctx, INTERNAL_SERVER_ERROR)
                    }
                }
              } finally {
                writePageLog(pageLog)
              }
            }
            // Обнуляем PageLog потому что вызов writePageLog в блоке finally произойдёт до того
            // как отработает действие с этим PageLog. Обнуление PageLog не позволит его преждевременно сохранить.
            PageLog.remove()

          case r =>
            logger.error("Unsupported result type: " + r)
            sendError(ctx, INTERNAL_SERVER_ERROR)
        }
      }
      handleResult(result, PageLog.get())

    } finally {
      writePageLog(PageLog.get())
      Thread.currentThread().setName("netty@" + Thread.currentThread().getId + ": [empty]")
    }

    /** Сохранить RequestLog в файл */
    def writePageLog(@Nullable pageLog: PageLog): Unit = {
      if (pageLog != null) {
        pageLog match {
          case null => ()
          case rlog =>
            if (!rlog.noLog) {
              rlog.setFinishedTime(System.currentTimeMillis())
              requestLogWriter.writeLn(rlog.toLogString)
              PageLog.remove()
            }
        }
      }
    }
  }


  private def sendError(ctx: ChannelHandlerContext, status: HttpResponseStatus) {
    val response: FullHttpResponse = new DefaultFullHttpResponse(
      HttpVersion.HTTP_1_1, status, Unpooled.copiedBuffer("Failure: " + status.toString + "\r\n", CharsetUtil.UTF_8))
    response.headers().set(CONTENT_TYPE, "text/plain; charset=UTF-8")

    // Close the connection as soon as the error message is sent.
    ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE)
  }
}
