/*
 * Copyright © 2023 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2023-02-11 11:09
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

package com.github.rwsbillyang.proxy.tunnel

import org.smartboot.socket.StateMachineEnum
import org.smartboot.socket.enhance.EnhanceAsynchronousChannelProvider
import org.smartboot.socket.extension.plugins.HeartPlugin
import org.smartboot.socket.extension.processor.AbstractMessageProcessor;
import org.smartboot.socket.transport.AioQuickClient
import org.smartboot.socket.transport.AioSession
import java.io.IOException
import java.nio.channels.AsynchronousChannelGroup
import java.util.concurrent.TimeUnit


/**
 * TUN 和 TAP 分别是虚拟的三层和二层网络设备，也就是说，我们可以从 TUN 拿到的就是 IP 层的网络数据包了，而 TAP 则是二层网络包，比如以太网包。
 * 因为我只打算对 IP 层的包进行处理（其实只打算处理 TCP 和 UDP）
 *
 * 与远程Server建立连接，形成一个tunnel
 * 发送: 对于来自tun设备中的IP报文，解析出目的IP和port、应用层数据data，通过tunnel连接发送给TunnelServer。
 * 远程TunnelClient解析出ip,port,data等信息, 并与目的ip:port 建立连接，发送给目的地
 *
 * 接收: 远程server的数据（应用层数据）收到目的地的响应数据，通过tunnel连接，发送给TunnelClient
 * TunnelClient得到的是应用层的数据，需要构建[IP Header + [TCP/UDP Header + data]]，写入tun设备
 * */
class TunnelClient(val server: String, port: Int) {
    private val processor = object : AbstractMessageProcessor<ByteArray>() {
        override fun process0(session: AioSession, msg: ByteArray) {
            //接收到响应消息，构建IP包放入tun设备
        }
        override fun stateEvent0(session: AioSession, stateMachineEnum: StateMachineEnum, throwable: Throwable?) {

        }
    }.also {
        it.addPlugin(object : HeartPlugin<ByteArray>(5, 7, TimeUnit.SECONDS) {
            @Throws(IOException::class)
            override fun sendHeartRequest(session: AioSession) {
                val writeBuffer = session.writeBuffer()
                writeBuffer.writeInt(0)
                writeBuffer.flush()
            }

            override fun isHeartMessage(session: AioSession, msg: ByteArray) = msg.size == 1 && msg[0].toInt() == 0
        })
    }

    private val client = AioQuickClient(server, port, RawDataProtocol(), processor)

    private lateinit var session: AioSession


    fun start(){
        val asynchronousChannelGroup: AsynchronousChannelGroup =
            EnhanceAsynchronousChannelProvider(true).openAsynchronousChannelGroup(
                Runtime.getRuntime().availableProcessors()
            ) { r -> Thread( r,"ClientGroup") }

        session = client.start(asynchronousChannelGroup)


        //        client.start<Any?>(null, object : CompletionHandler<AioSession, Any?> {
//            override fun completed(result: AioSession, attachment: Any?) {
//                println("success")
//                client.shutdown()
//            }
//
//            override fun failed(exc: Throwable, attachment: Any?) {
//                println("fail")
//                exc.printStackTrace()
//            }
//        })
    }

    fun write(ipPacket: ByteArray){
        if(session == null){
            throw Exception("not start TunnelClient")
        }
        session.writeBuffer()
    }

    fun read(ipPacket: ByteArray){

    }
}