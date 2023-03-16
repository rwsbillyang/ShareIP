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


import com.github.rwsbillyang.proxy.Config
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.socksx.v5.Socks5CommandRequestDecoder
import io.netty.handler.codec.socksx.v5.Socks5InitialRequestDecoder
import io.netty.handler.codec.socksx.v5.Socks5PasswordAuthRequestDecoder
import io.netty.handler.codec.socksx.v5.Socks5ServerEncoder


/**
 * refer to
 * https://gitee.com/kdyzm/socks5-netty
 * https://github.com/netty/netty/tree/4.1/example/src/main/java/io/netty/example/socksproxy
 * https://segmentfault.com/a/1190000038498680 Socks5 代理协议详解 & 基于 Netty 的实现
 */
class Socks5Server(private val config: Config) {
    //private val log = LoggerFactory.getLogger("Socks5Server")
    private var serverChannel: Channel? = null

    fun start() {
        val clientWorkGroup: EventLoopGroup = NioEventLoopGroup()
        val bossGroup: EventLoopGroup = NioEventLoopGroup()
        val workerGroup: EventLoopGroup = NioEventLoopGroup()
        val bootstrap = ServerBootstrap()

        bootstrap.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java)
            .option(ChannelOption.SO_BACKLOG, 128)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
            .childOption(ChannelOption.SO_KEEPALIVE, true)
            .childHandler(object : ChannelInitializer<NioServerSocketChannel>() {
                override fun initChannel(ch: NioServerSocketChannel) {
                    val pipeline = ch.pipeline()
                    //socks5响应最后一个encode
                    pipeline.addLast(Socks5ServerEncoder.DEFAULT)

                    //处理socks5初始化请求
                    pipeline.addLast(Socks5InitialRequestDecoder())
                    pipeline.addLast(Socks5InitialRequestHandler(config))

                    //处理认证请求
                    if (config.isAuth) {
                        pipeline.addLast(Socks5PasswordAuthRequestDecoder())
                        pipeline.addLast(Socks5PasswordAuthRequestHandler(config))
                    }

                    //处理connection请求
                    pipeline.addLast(Socks5CommandRequestDecoder())
                    pipeline.addLast(Socks5CommandRequestHandler(clientWorkGroup))

                    //channelPipeline.addLast(IdleStateHandler(3,0,0, TimeUnit.SECONDS))
                    //channelPipeline.addLast(HeartBeatServerHandler());

                }
            })

        try {
            val future: ChannelFuture = bootstrap.bind(config.socks5Port).sync()// 绑定端口启动成功

            println("socks5 netty server has started on port ${config.socks5Port}")

            future.channel().also {
                serverChannel = it
            }.closeFuture().sync()// 阻塞至channel关闭

        } catch (e: InterruptedException) {
            println("InterruptedException: ${e.message}")
        } catch(e: Exception){
            println("Exception: ${e.message}")
        } finally {
            bossGroup.shutdownGracefully()
            workerGroup.shutdownGracefully()
        }

    }

    fun close(){
        if(serverChannel != null){
            serverChannel!!.close()
            serverChannel = null
        }
    }
}
