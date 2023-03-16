/*
 * Copyright © 2022 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2022-10-14 20:13
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

package com.github.rwsbillyang.proxy.p2p

import com.github.rwsbillyang.proxy.ProxyManager
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.LengthFieldBasedFrameDecoder
import io.netty.handler.codec.LengthFieldPrepender
import io.netty.handler.codec.string.StringDecoder
import io.netty.handler.codec.string.StringEncoder
import io.netty.util.CharsetUtil




//普通的socket连接，用于p2p proxy list信息的
class PeerAsServer(private val proxyManager: ProxyManager): ChannelInitializer<SocketChannel>(){
    //private val log = LoggerFactory.getLogger("PeerAsServer")
    private var serverChannel: Channel? = null

    fun start() {
        val bossGroup: EventLoopGroup = NioEventLoopGroup()
        val workerGroup: EventLoopGroup = NioEventLoopGroup()
        try {
            val serverBootstrap = ServerBootstrap()

            serverBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel::class.java)
                .childHandler(this)

            // 绑定端口启动成功
            val channelFuture: ChannelFuture = serverBootstrap.bind(proxyManager.config!!.port).sync()
            channelFuture.channel().also {
                serverChannel = it
            }.closeFuture().sync()// 阻塞至channel关闭

        }catch(e: InterruptedException){
            println("InterruptedException: ${e.message}")
        }catch(e: Exception){
            println("Exception: ${e.message}")
        }finally {
            bossGroup.shutdownGracefully()
            workerGroup.shutdownGracefully()
        }
    }

    fun stop(){
        if(serverChannel != null){
            serverChannel!!.close()
            serverChannel = null
        }
    }

    override fun initChannel(ch: SocketChannel) {
        val pipeline: ChannelPipeline = ch.pipeline()

        pipeline.addLast(LengthFieldBasedFrameDecoder(Int.MAX_VALUE, 0, 4, 0, 4))
        pipeline.addLast(LengthFieldPrepender(4))
        pipeline.addLast(StringDecoder(CharsetUtil.UTF_8))//字符串解码
        pipeline.addLast(StringEncoder(CharsetUtil.UTF_8))//字符串编码

        pipeline.addLast(MyServerHandler())//自己定义的处理器
    }

    inner class MyServerHandler : SimpleChannelInboundHandler<String>() {

        //得到join信息，得到其发送过来的ip6，然后回复自己的proxies
        @Throws(java.lang.Exception::class)
        protected override fun channelRead0(ctx: ChannelHandlerContext, msg: String) {
            val ip = ctx.channel().remoteAddress().toString()
            println("clientIp=$ip, $msg")

            val response = proxyManager.handleRequest(msg, ip)
            if(response != null){
                ctx.channel().writeAndFlush(response)
            }
        }

        @Throws(java.lang.Exception::class)
        override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
            cause.printStackTrace()

            println(cause.message)

            ctx.close()
        }
    }
}
