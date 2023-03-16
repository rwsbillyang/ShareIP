/*
 * Copyright © 2022 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2022-10-14 22:30
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

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.socksx.v5.*


class Socks5CommandResponseHandler: SimpleChannelInboundHandler<DefaultSocks5CommandResponse>()  {

    override fun channelRead0(ctx: ChannelHandlerContext, msg: DefaultSocks5CommandResponse) {
        if (msg.decoderResult().isFailure) {
            System.err.println("Socks5CommandResponseHandler: msg.decoderResult failure")
            ctx.fireChannelRead(msg)
        }else{
            val status = msg.status()
            if(status == Socks5CommandStatus.FAILURE){
                System.err.println("wrong password")
            }else if(status== Socks5CommandStatus.SUCCESS){
                //connect to dest successful, data IO
            }else{
                System.err.println("not support status: ${status.toString()}")
            }

            ctx.pipeline().remove(this)
            ctx.pipeline().remove(Socks5CommandResponseDecoder::class.java)
        }
    }
}
