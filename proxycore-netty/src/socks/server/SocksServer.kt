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

import com.github.rwsbillyang.proxy.Config
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.socksx.SocksPortUnificationServerHandler
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler


class SocksServer {
    private var serverChannel: Channel? = null

    fun start(config: Config = Config()) {
        val bossGroup: EventLoopGroup = NioEventLoopGroup(1)
        val workerGroup: EventLoopGroup = NioEventLoopGroup()
        try {
            val b = ServerBootstrap()
            b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel::class.java)
                .handler(LoggingHandler(LogLevel.INFO))
                .childHandler(object: ChannelInitializer<SocketChannel>() {
                    @Throws(Exception::class)
                    override fun initChannel(ch: SocketChannel) {
                        ch.pipeline().addLast(
                            LoggingHandler(LogLevel.DEBUG),
                            SocksPortUnificationServerHandler(),
                            SocksServerHandler.INSTANCE
                        )
                    }
                })
            b.bind(config.port).sync().channel().also {
                serverChannel = it
            }.closeFuture().sync()
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
