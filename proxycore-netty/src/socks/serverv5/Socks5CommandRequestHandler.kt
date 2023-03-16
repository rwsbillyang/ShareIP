/*
 * Copyright © 2022 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2022-10-14 21:07
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

package com.github.rwsbillyang.proxy.socks5.server


import io.netty.bootstrap.Bootstrap
import io.netty.channel.*
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel

import io.netty.handler.codec.socksx.v5.*
import io.netty.util.ReferenceCountUtil


class Socks5CommandRequestHandler(private val eventExecutors: EventLoopGroup? = null,
                                  private val blackList: Set<String>? = null) :
    SimpleChannelInboundHandler<DefaultSocks5CommandRequest>() {
    //private val log = LoggerFactory.getLogger("Socks5CommandRequestHandler")


    override fun channelRead0(ctx: ChannelHandlerContext, msg: DefaultSocks5CommandRequest) {
        val socks5AddressType = msg.dstAddrType()
        if (msg.type() != Socks5CommandType.CONNECT) {
            println("receive commandRequest type=${msg.type()}")
            ReferenceCountUtil.retain(msg)
            ctx.fireChannelRead(msg)
            return
        }

        //检查黑名单
        if(isInBlackList(ctx, msg.dstAddr(), socks5AddressType))
            return

        println("prepare to connect dest，ip=${msg.dstAddr()},port=${msg.dstPort()}")

        val bootstrap = Bootstrap()
        bootstrap.group(eventExecutors)
            .channel(NioSocketChannel::class.java)
            .option(ChannelOption.TCP_NODELAY, true)
            .handler(object : ChannelInitializer<SocketChannel>() {
                @Throws(Exception::class)
                override fun initChannel(ch: SocketChannel) {
                    //添加服务端写客户端的Handler
                    ch.pipeline().addLast(Dest2ClientHandler(ctx))
                }
            })

        val future = bootstrap.connect(msg.dstAddr(), msg.dstPort())
        future.addListener {
            if (future.isSuccess) {
                println("successful connect dest")
                //添加客户端转发请求到服务端的Handler
                ctx.pipeline().addLast(Client2DestHandler(future))
                val commandResponse = DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS, socks5AddressType)
                ctx.writeAndFlush(commandResponse)
                ctx.pipeline().remove(Socks5CommandRequestHandler::class.java)
                ctx.pipeline().remove(Socks5CommandRequestDecoder::class.java)
            } else {
                println("fail to connect dest, address=${msg.dstAddr()},port=${msg.dstPort()}")
                val commandResponse = DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, socks5AddressType)
                ctx.writeAndFlush(commandResponse)
                future.channel().close()
            }
        }
    }





    private fun isInBlackList(ctx: ChannelHandlerContext, dstAddr: String,dstAddrType: Socks5AddressType): Boolean{
        //检查黑名单
        if (inBlackList(dstAddr)) {
            println("${dstAddr} 地址在黑名单中，拒绝连接")
            //假装连接成功
//            val commandResponse = DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS, socks5AddressType)
//            ctx.writeAndFlush(commandResponse)
//            ctx.pipeline().addLast("HttpServerCodec", HttpServerCodec())
//            ctx.pipeline().addLast(BlackListHandler())
//            ctx.pipeline().remove(Socks5CommandRequestHandler::class.java)
//            ctx.pipeline().remove(Socks5CommandRequestDecoder::class.java)

            val commandResponse = DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, dstAddrType)
            ctx.writeAndFlush(commandResponse)
            return true
        }
        return false
    }
    private fun inBlackList(dstAddr: String): Boolean {
        if(blackList.isNullOrEmpty()) return false

        for (black in blackList) {
            if (dstAddr.lowercase().endsWith(black)) {
                return true
            }
        }
        return false
    }
}
