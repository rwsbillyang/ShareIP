package com.github.rwsbillyang.proxy.socks5.server



import com.github.rwsbillyang.proxy.Config
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.socksx.v5.*
import io.netty.util.ReferenceCountUtil


class Socks5InitialRequestHandler(private val config: Config) :
    SimpleChannelInboundHandler<DefaultSocks5InitialRequest>() {

    //private val log = LoggerFactory.getLogger("Socks5InitialRequestHandler")

    @Throws(Exception::class)
    override fun channelRead0( ctx: ChannelHandlerContext, msg: DefaultSocks5InitialRequest) {
        println( "get socks5 initial request...")
        val failure = msg.decoderResult().isFailure
        if (failure) {
            println("fail to decode socks5 initial request")
            ReferenceCountUtil.retain(msg)
            ctx.fireChannelRead(msg)
            return
        }

        if (config.isAuth) {
            val socks5InitialResponse: Socks5InitialResponse =
                DefaultSocks5InitialResponse(Socks5AuthMethod.PASSWORD)
            ctx.writeAndFlush(socks5InitialResponse)
        } else {
            val socks5InitialResponse: Socks5InitialResponse =
                DefaultSocks5InitialResponse(Socks5AuthMethod.NO_AUTH)
            ctx.writeAndFlush(socks5InitialResponse)
        }
        ctx.pipeline().remove(this)
        ctx.pipeline().remove(Socks5InitialRequestDecoder::class.java)
    }
}
