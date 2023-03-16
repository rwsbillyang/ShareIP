/*
 * Copyright © 2022 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2022-11-09 22:04
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

import io.netty.bootstrap.Bootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.socksx.v4.Socks4ClientDecoder
import io.netty.handler.codec.socksx.v4.Socks4ClientEncoder
import io.netty.handler.codec.socksx.v5.Socks5ClientEncoder
import io.netty.handler.codec.socksx.v5.Socks5InitialResponseDecoder


class SocksClient {
    fun start() {
        val group = NioEventLoopGroup()
        val bootstrap = Bootstrap()
        bootstrap
            .group(group)
            .channel(NioSocketChannel::class.java)
            .option(ChannelOption.SO_BACKLOG, 128)
            .option(ChannelOption.SO_KEEPALIVE, true)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
            .handler(object : ChannelInitializer<NioSocketChannel>() {
                override fun initChannel(channel: NioSocketChannel) {
                    val pipeline = channel.pipeline()
                    if(AuthConfig.useV5)
                    {
                        pipeline.addLast(Socks5ClientEncoder.DEFAULT)
                        pipeline.addLast(Socks5InitialResponseDecoder())
                    }else
                    {
                        pipeline.addLast(Socks4ClientEncoder.INSTANCE, Socks4ClientDecoder())
                    }
                    pipeline.addLast(SocksClientHandler(devId, destDomain, destPort))
                }
            })

        try {
            //对通道关闭进行监听
            bootstrap.connect(socks5Server, socks5ServerPort).channel().closeFuture().sync()
        } catch (e: InterruptedException) {
            println("InterruptedException: ${e.message}")
        } catch (e: Exception) {
            println("Exception: ${e.message}")
        } finally {
            group.shutdownGracefully()
        }
    }

}
