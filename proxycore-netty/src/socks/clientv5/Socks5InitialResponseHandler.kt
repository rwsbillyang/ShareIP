/*
 * Copyright © 2022 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2022-10-14 22:19
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

package com.github.rwsbillyang.proxy.socks5.client

import com.github.rwsbillyang.proxy.Config
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.socksx.SocksVersion
import io.netty.handler.codec.socksx.v5.*


class Socks5InitialResponseHandler(private val devId: String, private val config: Config,
private val destServer: String, private val destPort: Int) : SimpleChannelInboundHandler<Socks5InitialResponse>() {

    override fun channelActive(ctx: ChannelHandlerContext) {
        ctx.writeAndFlush(DefaultSocks5InitialRequest(Socks5AuthMethod.PASSWORD))
    }

    override fun channelRead0(ctx: ChannelHandlerContext, msg: Socks5InitialResponse) {
        if (msg.decoderResult().isFailure) {
            System.err.println("Socks5InitialResponseHandler: msg.decoderResult failure")
            ctx.fireChannelRead(msg)
        } else {
            if (msg.version() == SocksVersion.SOCKS5) {
                if(msg.authMethod() == Socks5AuthMethod.PASSWORD){
                    val request = DefaultSocks5PasswordAuthRequest(devId, config.socks5Pwd)
                    ctx.writeAndFlush(request)
                }else{
                    ctx.writeAndFlush(DefaultSocks5CommandRequest(Socks5CommandType.CONNECT,
                        Socks5AddressType.DOMAIN, destServer, destPort))
                }
            } else {
                System.err.println("not support socks version: ${msg.version()}")
                ctx.fireChannelRead(msg)
            }

            ctx.pipeline().remove(this)
            ctx.pipeline().remove(Socks5InitialResponseDecoder::class.java)
        }
    }
}
