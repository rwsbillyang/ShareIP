package com.github.rwsbillyang.proxy.socks5.server
//
//
//import io.netty.channel.ChannelFutureListener
//import io.netty.channel.ChannelHandlerContext
//import io.netty.channel.ChannelInboundHandlerAdapter
//import io.netty.channel.DefaultFileRegion
//import io.netty.handler.codec.http.*
//import org.slf4j.LoggerFactory
//
//import java.io.File
//import java.io.RandomAccessFile
//
//
//
//class BlackListHandler : ChannelInboundHandlerAdapter() {
//    private val log = LoggerFactory.getLogger("BlackListHandler")
//
//    @Throws(Exception::class)
//    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
//        if (msg !is HttpRequest) {
//            log.info( "非http请求，直接关闭channel")
//            ctx.channel().close()
//            return
//        }
//
//
//        log.debug( "请求方式：${msg.method().name()}")
//        log.debug( "请求uri：${msg.uri()}")
//        if ("/favicon.ico".equals(msg.uri(), ignoreCase = true)) {
//            log.debug( "不处理 /favicon.ico 请求")
//            return
//        }
//
//        val resource = this.javaClass.classLoader!!.getResource("./blacklist.html")!!
//
//        val file =  RandomAccessFile(File(resource.file), "r")
//
//        val response: HttpResponse = DefaultHttpResponse(msg.protocolVersion(), HttpResponseStatus.OK)
//        response.headers()[HttpHeaderNames.CONTENT_TYPE] = "text/html; charset=UTF-8"
//        val keepAlive = HttpUtil.isKeepAlive(msg)
//        if (keepAlive) {
//            response.headers()[HttpHeaderNames.CONTENT_LENGTH] = file.length()
//            response.headers()[HttpHeaderNames.CONNECTION] = HttpHeaderValues.KEEP_ALIVE
//        }
//        ctx.write(response)
//        ctx.write(DefaultFileRegion(file.channel, 0, file.length()))
//        val future = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT)
//        if (!keepAlive) {
//            future.addListener(ChannelFutureListener.CLOSE)
//        }
//        file.close()
//        ctx.flush()
//    }
//
//    @Throws(Exception::class)
//    override fun channelInactive(ctx: ChannelHandlerContext) {
//        ctx.channel().close()
//    }
//}
