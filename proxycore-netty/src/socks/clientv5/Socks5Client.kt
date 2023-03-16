/*
 * Copyright © 2022 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2022-10-14 22:38
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
import com.github.rwsbillyang.proxy.socks.AuthConfig
import io.netty.bootstrap.Bootstrap
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.socksx.v5.Socks5ClientEncoder
import io.netty.handler.codec.socksx.v5.Socks5CommandResponseDecoder
import io.netty.handler.codec.socksx.v5.Socks5InitialResponseDecoder
import io.netty.handler.codec.socksx.v5.Socks5PasswordAuthResponseDecoder
import java.util.concurrent.TimeUnit

/**
 * 每连接一个远程dest，需创建一个client，多次阻塞调用connect代理请求目的地址
 * heart beat: https://juejin.cn/post/7035066135112319012
 * 理解socks5协议的工作过程和协议细节 https://wiyi.org/socks5-protocol-in-deep.html
 * implement reference: https://github.com/fengyouchao/sockslib
 * */
class Socks5Client(
    val devId: String,
    private val socks5Server: String,
    private val socks5ServerPort: Int,
    val destDomain: String,
    val destPort: Int,
    val config: Config = Config()) {
    private val bootstrap = Bootstrap()
    private val group = NioEventLoopGroup()

    fun start() {
        bootstrap.group(group).channel(NioSocketChannel::class.java)
            .option(ChannelOption.SO_BACKLOG, 128)
            .option(ChannelOption.SO_KEEPALIVE, true)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
            .handler(object : ChannelInitializer<NioSocketChannel>() {
                override fun initChannel(ch: NioSocketChannel) {
                    val pipeline = ch.pipeline()
                    pipeline.addLast(Socks5ClientEncoder.DEFAULT)

                    pipeline.addLast(Socks5InitialResponseDecoder())
                    pipeline.addLast(Socks5InitialResponseHandler(devId, config,destDomain, destPort))

                    if (AuthConfig.needAuth) {
                        pipeline.addLast(Socks5PasswordAuthResponseDecoder())
                        pipeline.addLast(Socks5AuthResponseHandler(destDomain, destPort))
                    }

                    pipeline.addLast(Socks5CommandResponseDecoder())
                    pipeline.addLast(Socks5CommandResponseHandler())
                }
            })
    }

    //启动客户端去连接服务器端
    fun connect() {
        println("netty client connect...")
        val channelFuture: ChannelFuture = bootstrap.connect(socks5Server, socks5ServerPort)
        channelFuture.addListener(object : ChannelFutureListener {
            override fun operationComplete(future: ChannelFuture) {
                if (!future.isSuccess) {
                    future.channel().eventLoop().schedule({
                        System.err.println("reconnect to server...")
                        connect()
                    }, 3000, TimeUnit.MILLISECONDS)
                } else {
                    println("successfully to connect server")
                }
            }
        })

        try {
            //对通道关闭进行监听
            channelFuture.channel().closeFuture().sync()
        } catch (e: InterruptedException) {
            println("InterruptedException: ${e.message}")
        } catch (e: Exception) {
            println("Exception: ${e.message}")
        } finally {
            group.shutdownGracefully()
        }
    }
}




