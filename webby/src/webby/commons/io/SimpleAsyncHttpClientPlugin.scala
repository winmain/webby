package webby.commons.io

import java.net.URI
import java.util.concurrent.TimeUnit

import io.netty.bootstrap.Bootstrap
import io.netty.channel._
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.http._
import webby.api.{Application, Plugin}
import webby.mvc.AppPluginHolder

/**
  * Плагин для совершения простых HTTP GET запросов, без ожидания результата.
  * Например, он подходит для обновления кеша на другом сервере.
  */
class SimpleAsyncHttpClientPlugin(app: Application) extends Plugin {
  private var bootstrap: Bootstrap = _
  private var group: EventLoopGroup = _

  /**
    * Called when the application starts.
    */
  override def onStart(): Unit = {
    group = new NioEventLoopGroup(4)
    bootstrap = new Bootstrap
    bootstrap.group(group)
      .channel(classOf[NioSocketChannel])
      .handler(new ChannelInitializer[SocketChannel] {
        override def initChannel(ch: SocketChannel): Unit = {
          val p: ChannelPipeline = ch.pipeline
          p.addLast(new HttpClientCodec)
          p.addLast(new ChannelInboundHandlerAdapter {
            override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
              // Handle and throw away channel exceptions
              if (ctx.channel().isActive) ctx.channel().close()
            }
          })
        }
      })
  }

  /**
    * Called when the application stops.
    */
  override def onStop(): Unit = {
    bootstrap = null
    if (group != null) group.shutdownGracefully(0, 1, TimeUnit.SECONDS)
  }

  def doRequest(req: FullHttpRequest): Unit = {
    require(bootstrap != null, "SimpleAsyncHttpClientPlugin not initialized")
    val uri: URI = new URI(req.uri)
    val port: Int = if (uri.getPort == -1) 80 else uri.getPort
    bootstrap.connect(uri.getHost, port).addListener(new ChannelFutureListener {
      override def operationComplete(future: ChannelFuture): Unit = {
        future.channel().writeAndFlush(req)
      }
    })
  }
}

object SimpleAsyncHttpClient {
  val holder = new AppPluginHolder[SimpleAsyncHttpClientPlugin]()

  def doGetRequest(url: String): Unit = {
    val req: DefaultFullHttpRequest =
      new DefaultFullHttpRequest(HttpVersion.HTTP_1_0, HttpMethod.GET, url)
    holder.get.doRequest(req)
  }
}
