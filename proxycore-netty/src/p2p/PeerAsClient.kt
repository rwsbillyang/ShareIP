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
import io.netty.bootstrap.Bootstrap
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.LengthFieldBasedFrameDecoder
import io.netty.handler.codec.LengthFieldPrepender
import io.netty.handler.codec.string.StringDecoder
import io.netty.handler.codec.string.StringEncoder
import io.netty.util.CharsetUtil


//https://github.com/libp2p/jvm-libp2p/tree/develop/examples
//P2P技术详解(二)：P2P中的NAT穿越(打洞)方案详解 http://www.52im.net/thread-542-1-1.html
class PeerAsClient(private val proxyManager: ProxyManager, private val msgToSend: String): ChannelInitializer<SocketChannel>() {
    //private val log = LoggerFactory.getLogger("PeerAsClient")

    /**
     * return true if successful
     * */
    fun connectToServer(server: String, port: Int): Boolean {
        var flag = false
        val group: EventLoopGroup = NioEventLoopGroup()
        try {
            val bootstrap = Bootstrap()
            bootstrap.group(group).channel(NioSocketChannel::class.java).handler(this)
            val channelFuture = bootstrap.connect(server, port).sync()

            channelFuture.channel().closeFuture().sync()

            flag = true
        }catch (e: InterruptedException){
            println("InterruptedException: ${e.message}")
        }catch(e: Exception){
            println("Exception: ${e.message}")
        }finally {
            group.shutdownGracefully()
        }
        return flag
    }

    override fun initChannel(ch: SocketChannel) {
        val pipeline: ChannelPipeline = ch.pipeline()

        pipeline.addLast(LengthFieldBasedFrameDecoder(Int.MAX_VALUE, 0, 4, 0, 4))
        pipeline.addLast(LengthFieldPrepender(4))
        pipeline.addLast(StringDecoder(CharsetUtil.UTF_8))
        pipeline.addLast(StringEncoder(CharsetUtil.UTF_8))
        pipeline.addLast(MyClientHandler())
    }

    inner class MyClientHandler : SimpleChannelInboundHandler<String>() {
        /**
         * 当服务器端与客户端进行建立连接的时候会触发，如果没有触发读写操作，则服务端和客户端之间不会进行数据通信，
         * 也就是channelRead0不会执行，当通道连接的时候，触发channelActive方法向服务端发送数据触发服务器端的
         * handler的channelRead0回调，然后服务端向客户端发送数据触发客户端的channelRead。
         */
        @Throws(java.lang.Exception::class)
        override fun channelActive(ctx: ChannelHandlerContext) {
            ctx.writeAndFlush(msgToSend)
        }

        /**
         * server回复给自己的proxies列表
         */
        @Throws(java.lang.Exception::class)
        protected override fun channelRead0(ctx: ChannelHandlerContext, msg: String) {
            println(ctx.channel().remoteAddress().toString()+ "returns : $msg")
            proxyManager.handleResponse(msg)
            ctx.channel().close()
        }


        @Throws(java.lang.Exception::class)
        override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
            cause.printStackTrace()
            println(cause.message)
            ctx.close()
        }
    }
}
