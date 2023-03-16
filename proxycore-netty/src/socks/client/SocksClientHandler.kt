/*
 * Copyright © 2022 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2022-11-09 22:06
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

package com.github.rwsbillyang.proxy.socks.client

import com.github.rwsbillyang.proxy.socks.AuthConfig
import com.github.rwsbillyang.proxy.socks.server.SocksServerUtils

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.socksx.SocksMessage
import io.netty.handler.codec.socksx.SocksVersion
import io.netty.handler.codec.socksx.v4.*
import io.netty.handler.codec.socksx.v5.*

class SocksClientHandler(val devId: String, val destServer: String, val destPort: Int) : SimpleChannelInboundHandler<SocksMessage>() {

    override fun channelActive(ctx: ChannelHandlerContext) {
        ctx.write(
            if(AuthConfig.useV5)
                DefaultSocks5InitialRequest(Socks5AuthMethod.NO_AUTH,Socks5AuthMethod.PASSWORD)
            else
                DefaultSocks4CommandRequest(Socks4CommandType.CONNECT,destServer, destPort)
        )
    }


    @Throws(Exception::class)
    override fun channelRead0(ctx: ChannelHandlerContext, socksResponse: SocksMessage) {
        when (socksResponse.version()) {
            SocksVersion.SOCKS4a -> {
                if(socksResponse is Socks4CommandResponse) {
                    val status = socksResponse.status()
                    if(status == Socks4CommandStatus.SUCCESS){
                        //connection established
                        ctx.pipeline().addLast(SocksClientConnectHandler())
                        ctx.pipeline().remove(this) //移除自己
                    }else{
                        System.err.println("wrong, Socks4CommandResponse.status=$status")
                        ctx.close()
                    }
                }else{
                    System.err.println("not support v4 response type")
                    ctx.close()
                }
            }

            SocksVersion.SOCKS5 -> if (socksResponse is Socks5InitialResponse) {
                if(socksResponse.authMethod() == Socks5AuthMethod.PASSWORD){
                    ctx.pipeline().addFirst(Socks5PasswordAuthResponseDecoder())
                    ctx.write(DefaultSocks5PasswordAuthRequest(devId, AuthConfig.password))
                }else{
                    ctx.pipeline().addFirst(Socks5CommandResponseDecoder())
                    ctx.write(DefaultSocks5CommandRequest(Socks5CommandType.CONNECT,
                        Socks5AddressType.DOMAIN, destServer, destPort))
                }
            } else if (socksResponse is Socks5PasswordAuthResponse) {
                val status = socksResponse.status()
                if(status== Socks5PasswordAuthStatus.SUCCESS){
                    ctx.pipeline().addFirst(Socks5CommandResponseDecoder())
                    ctx.write(DefaultSocks5CommandRequest(Socks5CommandType.CONNECT,
                        Socks5AddressType.DOMAIN, destServer, destPort))
                }else{
                    System.err.println("wrong, Socks5PasswordAuthResponse.status=$status")
                    ctx.close()
                }
            } else if (socksResponse is Socks5CommandResponse) {
                val status = socksResponse.status()
                if(status == Socks5CommandStatus.SUCCESS){
                    //connection established
                    ctx.pipeline().addLast(SocksClientConnectHandler())
                    ctx.pipeline().remove(this) //移除自己
                }else{
                    System.err.println("wrong, Socks5CommandResponse.status=$status")
                    ctx.close()
                }
            } else {
                System.err.println("not support v5 response type")
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
