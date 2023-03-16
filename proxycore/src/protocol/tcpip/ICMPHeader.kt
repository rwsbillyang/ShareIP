/*
 * Copyright © 2023 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2023-02-16 11:50
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

package com.github.rwsbillyang.proxy.protocol.tcpip

import com.github.rwsbillyang.proxy.protocol.ProtocolHeader

/**
0                   1                   2                   3
0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|     Type      |     Code      |          Checksum             |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                             unused                            |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|      Internet Header + 64 bits of Original Data Datagram      |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * https://www.rfc-editor.org/rfc/rfc792
 * https://nmap.org/book/tcpip-ref.html
 * https://www.cnblogs.com/iiiiher/p/8513748.html
 * */
class ICMPHeader(data: ByteArray, size: Int, offset: Int = 0) : ProtocolHeader(data, offset) {
    companion object{
        const val Type = "Type"
        const val Code = "Code"
        const val Checksum = "Checksum"
        const val HeaderData = "HeaderData"
        const val Optional = "Optional"
    }

    //有多个bits且不能被8整除时，需提供bitMask，bits小于0时，需提供dynamicBits
    override val fields = listOf(
        f(Type,  8,  "TYPE 占8位 用于标识这个ICMP报文具体是属于哪一种类型"),
        f(Code,  8, "CODE 占8位 提供了有关报文类型的更多信息"),
        f(Checksum, 16,  "CHECKSUM 占16位 校验和字段。ICMP使用与IP相同的校验和算法，但是ICMP校验和的计算只包括ICMP报文"),
        f(HeaderData, 32),
        f(Optional, -1,  "optional payload"){
            (size - 8)*8
        },
    )
}