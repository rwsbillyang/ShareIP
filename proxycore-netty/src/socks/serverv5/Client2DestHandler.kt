package com.github.rwsbillyang.proxy.socks5.server



import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter



class Client2DestHandler(private val dstChannelFuture: ChannelFuture) :  ChannelInboundHandlerAdapter() {
    //private val log = LoggerFactory.getLogger("Client2DestHandler")

    @Throws(Exception::class)
    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        dstChannelFuture.channel().writeAndFlush(msg)
    }

    @Throws(Exception::class)
    override fun channelInactive(ctx: ChannelHandlerContext) {
        println("connection between client and proxy closed, to close connection between proxy and dest")
        dstChannelFuture.channel().close()
    }

    @Throws(Exception::class)
    override fun exceptionCaught(
        ctx: ChannelHandlerContext,
        cause: Throwable
    ) {
        System.err.println("exception: ${cause.message}")
    }

}
