/*
 * Copyright © 2022 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2022-11-03 15:51
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

import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.util.concurrent.Promise


class DirectClientHandler(private val promise: Promise<Channel>) : ChannelInboundHandlerAdapter() {
    override fun channelActive(ctx: ChannelHandlerContext) {
        ctx.pipeline().remove(this)
        promise.setSuccess(ctx.channel())
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, throwable: Throwable) {
        promise.setFailure(throwable)
    }
}
