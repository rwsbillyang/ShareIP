/*
 * Copyright © 2023 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2023-02-11 12:19
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


import com.github.rwsbillyang.proxy.HeaderUtil
import com.github.rwsbillyang.proxy.protocol.toIp
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.smartboot.socket.StateMachineEnum
import org.smartboot.socket.extension.plugins.HeartPlugin
import org.smartboot.socket.extension.processor.AbstractMessageProcessor
import org.smartboot.socket.transport.AioQuickClient
import org.smartboot.socket.transport.AioQuickServer
import org.smartboot.socket.transport.AioSession
import java.io.IOException
import java.util.concurrent.TimeUnit





/**
 * 对客户端发送过来的数据，转发给目的地
 * 对目的地回复过来的数据，转发给客户端
 * */
class TunnelServer(val port: Int) {
    val log: Logger = LoggerFactory.getLogger("TunnelServer")

    private val relayProcessor = object : AbstractMessageProcessor<TunnelData>(){
        override fun process0(session: AioSession, msg: TunnelData) {
            log.info("recv req: ${msg.toString()}")

            val relayClient = AioQuickClient(msg.dest.toIp(), msg.port?.toInt()?:80, RawDataProtocol()){ _, data ->
                log.info("recv rep")
                with(session.writeBuffer()){
                    write(data)//目的地返回的响应，回传给源请求者
                    flush()
                }
            }
            //发送给目的地
            with(relayClient.start().writeBuffer()){
                if(msg.data != null) write(msg.data)
                flush()
            }

            relayClient.shutdownNow()
        }
        override fun stateEvent0(session: AioSession, stateMachineEnum: StateMachineEnum, throwable: Throwable?) {
            
        }
    }.also {
        it.addPlugin(object : HeartPlugin<TunnelData>(5, 7, TimeUnit.SECONDS) {
            @Throws(IOException::class)
            override fun sendHeartRequest(session: AioSession) {
                val writeBuffer = session.writeBuffer()
                writeBuffer.writeInt(0)
                writeBuffer.flush()
            }

            override fun isHeartMessage(session: AioSession, msg: TunnelData) = msg.dest == 0
        })
    }


    private val server = AioQuickServer(port, TunnelProtocol(), relayProcessor)

    fun start(){
        server.setBannerEnabled(true)
        server.setReadBufferSize(8092)
        server.setWriteBuffer(1024, 8)
        server.setLowMemory(true)//https://smartboot.gitee.io/smart-socket/low-memory.html
        server.start()
    }

    fun shutdown(){
        server.shutdown()
    }
}
