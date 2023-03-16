/*
 * Copyright © 2022 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2022-11-03 16:01
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.rwsbillyang.proxy.socks.server

import com.github.rwsbillyang.proxy.socks.AuthConfig
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.socksx.SocksMessage
import io.netty.handler.codec.socksx.SocksVersion
import io.netty.handler.codec.socksx.v4.Socks4CommandRequest
import io.netty.handler.codec.socksx.v4.Socks4CommandType
import io.netty.handler.codec.socksx.v5.*


@ChannelHandler.Sharable
class SocksServerHandler private constructor() : SimpleChannelInboundHandler<SocksMessage>() {
    companion object {
        val INSTANCE = SocksServerHandler()
    }

    @Throws(Exception::class)
    override fun channelRead0(ctx: ChannelHandlerContext, socksRequest: SocksMessage) {
        when (socksRequest.version()) {
            SocksVersion.SOCKS4a -> {
                val socksV4CmdRequest = socksRequest as Socks4CommandRequest
                if (socksV4CmdRequest.type() === Socks4CommandType.CONNECT) {
                    ctx.pipeline().addLast(SocksServerConnectHandler())
                    ctx.pipeline().remove(this) //移除自己
                    ctx.fireChannelRead(socksRequest)
                } else {
                    ctx.close()
                }
            }

            SocksVersion.SOCKS5 -> if (socksRequest is Socks5InitialRequest) {
                if (AuthConfig.needAuth){
                    ctx.pipeline().addFirst(Socks5PasswordAuthRequestDecoder());
                    ctx.write(DefaultSocks5InitialResponse(Socks5AuthMethod.PASSWORD));
                }else{
                    ctx.pipeline().addFirst(Socks5CommandRequestDecoder())
                    ctx.write(DefaultSocks5InitialResponse(Socks5AuthMethod.NO_AUTH))
                }
            } else if (socksRequest is Socks5PasswordAuthRequest) {
                ctx.pipeline().addFirst(Socks5CommandRequestDecoder())
                ctx.write(
                    DefaultSocks5PasswordAuthResponse(
                    if (AuthConfig.password == socksRequest.password().trim())
                        Socks5PasswordAuthStatus.SUCCESS
                    else Socks5PasswordAuthStatus.FAILURE)
                )
            } else if (socksRequest is Socks5CommandRequest) {
                if (socksRequest.type() === Socks5CommandType.CONNECT) {
                    ctx.pipeline().addLast(SocksServerConnectHandler())
                    ctx.pipeline().remove(this) //移除自己
                    ctx.fireChannelRead(socksRequest)
                } else {
                    ctx.close()
                }
            } else {
                ctx.close()
            }

            SocksVersion.UNKNOWN -> ctx.close()
            null -> ctx.close()
        }
    }

    override fun channelReadComplete(ctx: ChannelHandlerContext) {
        ctx.flush()
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, throwable: Throwable) {
        throwable.printStackTrace()
        SocksServerUtils.closeOnFlush(ctx.channel())
    }


}
