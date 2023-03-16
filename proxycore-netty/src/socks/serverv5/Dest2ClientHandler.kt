package com.github.rwsbillyang.proxy.socks5.server



import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter


class Dest2ClientHandler(private val clientChannelHandlerContext: ChannelHandlerContext) :
    ChannelInboundHandlerAdapter() {

    //private val log = LoggerFactory.getLogger("Dest2ClientHandler")

    @Throws(Exception::class)
    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        clientChannelHandlerContext.writeAndFlush(msg)
    }

    @Throws(Exception::class)
    override fun channelInactive(ctx: ChannelHandlerContext) {
        println("connection between proxy and destination closed, to close connection between client and proxy")
        clientChannelHandlerContext.channel().close()
    }

    @Throws(Exception::class)
    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        System.err.println("exception: ${cause.message}")
    }

}
