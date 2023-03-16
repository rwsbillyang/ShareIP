/*
 * Copyright © 2023 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2023-02-12 20:36
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

import org.smartboot.socket.Protocol
import org.smartboot.socket.transport.AioSession
import java.nio.ByteBuffer

class RawDataProtocol: Protocol<ByteArray> {
    override fun decode(readBuffer: ByteBuffer, session: AioSession): ByteArray {
        val buffer = ByteArray(readBuffer.remaining())
        readBuffer.get(buffer)
        return buffer
    }
}