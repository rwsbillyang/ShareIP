/*
 * Copyright © 2023 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2023-02-11 18:15
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
import org.smartboot.socket.buffer.BufferPagePool
import org.smartboot.socket.extension.plugins.MonitorPlugin
import org.smartboot.socket.extension.processor.AbstractMessageProcessor
import org.smartboot.socket.transport.AioSession
import org.smartboot.socket.transport.UdpBootstrap


class TunnelUdpServer {
    fun start(){
        val processor: AbstractMessageProcessor<TunnelData> = object : AbstractMessageProcessor<TunnelData>() {
            override fun process0(session: AioSession, msg: TunnelData) {


            }

            override fun stateEvent0(session: AioSession, stateMachineEnum: StateMachineEnum, throwable: Throwable?) {
                throwable?.printStackTrace()

            }
        }
        processor.addPlugin(MonitorPlugin<TunnelData>(5, true))
        val bootstrap = UdpBootstrap(TunnelProtocol(), processor)
        bootstrap.setThreadNum(Runtime.getRuntime().availableProcessors())
            .setBufferPagePool(BufferPagePool(1024 * 1024 * 16, Runtime.getRuntime().availableProcessors(), true))
            .setReadBufferSize(1024).open(8888)
    }
}