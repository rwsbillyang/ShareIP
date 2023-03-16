package com.github.rwsbillyang.proxy.socks5.server


import com.github.rwsbillyang.proxy.Config
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.socksx.v5.*


class Socks5PasswordAuthRequestHandler(private val config: Config) :
    SimpleChannelInboundHandler<DefaultSocks5PasswordAuthRequest>() {

    @Throws(Exception::class)
    override fun channelRead0(ctx: ChannelHandlerContext, msg: DefaultSocks5PasswordAuthRequest) {
        //认证成功
        if (config.socks5Pwd == msg.password().trim()) {
            val passwordAuthResponse: Socks5PasswordAuthResponse = DefaultSocks5PasswordAuthResponse(Socks5PasswordAuthStatus.SUCCESS)
            ctx.writeAndFlush(passwordAuthResponse)
            ctx.pipeline().remove(this)
            ctx.pipeline().remove(Socks5PasswordAuthRequestDecoder::class.java)
            return
        }
        val passwordAuthResponse: Socks5PasswordAuthResponse = DefaultSocks5PasswordAuthResponse(Socks5PasswordAuthStatus.FAILURE)
        //发送鉴权失败消息，完成后关闭channel
        ctx.writeAndFlush(passwordAuthResponse).addListener(ChannelFutureListener.CLOSE)
    }
}
