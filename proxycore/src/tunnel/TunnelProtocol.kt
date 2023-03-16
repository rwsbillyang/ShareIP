/*
 * Copyright © 2023 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2023-02-11 12:22
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

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.smartboot.socket.Protocol
import org.smartboot.socket.transport.AioSession
import java.nio.ByteBuffer

class TunnelProtocol: Protocol<TunnelData> {
    val log: Logger = LoggerFactory.getLogger("TunnelProtocol")
    override fun decode(readBuffer: ByteBuffer, session: AioSession): TunnelData? {
        if (readBuffer.remaining() < Int.SIZE_BYTES) {
            return null
        }

        //mark属性，这个属性是一个标识的作用，即记录当前position的位置，
        // 在后续如果调用reset()或者flip()方法时，ByteBuffer的position就会被重置到mark所记录的位置。
        readBuffer.mark()

        val dest = readBuffer.int
        if(dest == 0)
            return TunnelData(dest, null, null) //hear beat msg

        if (readBuffer.remaining() < Short.SIZE_BYTES * 2) {
            return null
        }

        val port = readBuffer.short
        val length = readBuffer.short.toInt()
        if (length > readBuffer.remaining()) {
            readBuffer.reset()
            return null
        }
        if(length == 0){
            return TunnelData(dest, port, null)
        }else{
            val d = ByteArray(length.toInt())
            readBuffer.get(d)
            return TunnelData(dest, port, d)
        }
    }

    fun encode(buffer: ByteBuffer, payload: TunnelData){
        buffer.putInt(payload.dest)
        if(payload.dest > 0){
            if(payload.port == null){
                log.warn("no port in TunnelData, set default 80")
            }
            buffer.putShort(payload.port ?: 80)
            if(payload.data == null)
            {
                buffer.putShort(0)
            }else{
                buffer.putShort(payload.data.size.toShort())
                buffer.put(payload.data)
            }
        }
    }
}