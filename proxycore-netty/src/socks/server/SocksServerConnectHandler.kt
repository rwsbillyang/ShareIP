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


import com.github.rwsbillyang.proxy.socks.server.SocksServerUtils.closeOnFlush
import io.netty.bootstrap.Bootstrap
import io.netty.channel.*
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.socksx.SocksMessage
import io.netty.handler.codec.socksx.v4.DefaultSocks4CommandResponse
import io.netty.handler.codec.socksx.v4.Socks4CommandRequest
import io.netty.handler.codec.socksx.v4.Socks4CommandStatus
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandResponse
import io.netty.handler.codec.socksx.v5.Socks5AddressType
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus
import io.netty.util.concurrent.Future
import io.netty.util.concurrent.FutureListener


@ChannelHandler.Sharable
class SocksServerConnectHandler : SimpleChannelInboundHandler<SocksMessage>() {
    private val b = Bootstrap()

    @Throws(Exception::class)
    override fun channelRead0(ctx: ChannelHandlerContext, message: SocksMessage) {
        if (message is Socks4CommandRequest) {
            bridge(ctx, message.dstAddr(), message.dstPort())
        } else if (message is Socks5CommandRequest) {
            bridge(ctx, message.dstAddr(), message.dstPort(), true, message.dstAddrType())
        } else {
            ctx.close()
        }
    }

    @Throws(Exception::class)
    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        closeOnFlush(ctx.channel())
    }

    private fun bridge(ctx: ChannelHandlerContext, dest: String, destPort: Int, isV5: Boolean = false, destType: Socks5AddressType? = null){
        //待新建channel，socksServer到目的地的连接channel
        val promise = ctx.executor().newPromise<Channel>()

        //socksServer到目的地连接成功后回调
        promise.addListener(//socksServer到目的地的连接成功后执行DirectClientHandler，一旦连接active，将设置成功结果，然后返回，执行此Listener
            object : FutureListener<Channel> {
                @Throws(Exception::class)
                override fun operationComplete(future: Future<Channel>) {
                    val outboundChannel = future.now//即新建的channel：socksServer到目的地的连接channel
                    if (future.isSuccess) {
                        val responseFuture = ctx.channel().writeAndFlush(
                            if(isV5)
                                DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS, destType, dest, destPort)
                            else
                                DefaultSocks4CommandResponse(Socks4CommandStatus.SUCCESS)
                        )
                        //原pipeline上写入response，成功后回调：移除当自前的handler，由RelayHandler处理
                        responseFuture.addListener(ChannelFutureListener {
                            ctx.pipeline().remove(this@SocksServerConnectHandler)
                            outboundChannel.pipeline().addLast(RelayHandler(ctx.channel()))//socksServer到目的地的连接channel的数据转交给RelayHandler，即socksServer到客户端
                            ctx.pipeline().addLast(RelayHandler(outboundChannel))//客户端到目的地的数据，将转交给outboundChannel：socksServer到目的地的连接channel
                        })
                    } else {
                        ctx.channel().writeAndFlush(
                            if(isV5)
                                DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, destType)
                            else
                                DefaultSocks4CommandResponse(Socks4CommandStatus.REJECTED_OR_FAILED)
                        )
                        closeOnFlush(ctx.channel())
                    }
                }
            })

        val inboundChannel = ctx.channel()
        b.group(inboundChannel.eventLoop())
            .channel(NioSocketChannel::class.java)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
            .option(ChannelOption.SO_KEEPALIVE, true)
            .handler(DirectClientHandler(promise))
        //socksServer 到目的地的连接，连接成功后DirectClientHandler处理：将promise设置为成功，成功结果就是socksServer到目的地的连接的channel
        b.connect(dest, destPort).addListener(ChannelFutureListener { future ->
            if (future.isSuccess) {
                // Connection established use handler provided results
            } else {
                // Close the connection if the connection attempt has failed.
                ctx.channel().writeAndFlush(
                    if(isV5)
                        DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, destType)
                    else
                        DefaultSocks4CommandResponse(Socks4CommandStatus.REJECTED_OR_FAILED)
                )
                closeOnFlush(ctx.channel())
            }
        })
    }
}
