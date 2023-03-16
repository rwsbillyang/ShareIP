/*
 * Copyright © 2022 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2022-10-14 23:46
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

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.timeout.IdleState
import io.netty.handler.timeout.IdleStateEvent

class HeartBeatServerHandler : SimpleChannelInboundHandler<String>() {
    var readIdleTimes = 0
    @Throws(Exception::class)
    override  fun channelRead0(ctx: ChannelHandlerContext, s: String) {
        println(" ====== > [server] message received : $s")
        if ("Heartbeat Packet" == s) {
            ctx.channel().writeAndFlush("ok")
        } else {
            println(" 其他信息处理...")
        }
    }

    @Throws(Exception::class)
    override fun userEventTriggered(ctx: ChannelHandlerContext, evt: Any) {
        val event: IdleStateEvent = evt as IdleStateEvent
        var eventType: String? = null
        when (event.state()) {
            IdleState.READER_IDLE -> {
                eventType = "读空闲"
                readIdleTimes++ // 读空闲的计数加1
            }
            IdleState.WRITER_IDLE -> eventType = "写空闲"
            IdleState.ALL_IDLE -> eventType = "读写空闲"
        }
        println(ctx.channel().remoteAddress().toString() + "超时事件：" + eventType)
        if (readIdleTimes > 3) {
            println(" [server]读空闲超过3次，关闭连接，释放更多资源")
            ctx.channel().writeAndFlush("idle close")
            ctx.channel().close()
        }
    }

    @Throws(Exception::class)
    override fun channelActive(ctx: ChannelHandlerContext) {
        System.err.println("=== " + ctx.channel().remoteAddress() + " is active ===")
    }
}

