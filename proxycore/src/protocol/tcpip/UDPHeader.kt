/*
 * Copyright © 2023 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2023-02-14 14:59
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
0            |        1       |        2      |        3
0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
Source  Port           |         Dest    Port
-----------------------------------------------------------------
Data Length            |          Checksum
-----------------------------------------------------------------
Data
...
-----------------------------------------------------------------

 * Source Port(源端口号): 在需要对方回信时选用。不需要时可用全0
 * Dest Port(目标端口号): 在终点交付时必须使用到
 * Data Length(UDP数据报长度): 长度包含首部和数据, 最小值为8字节
 * Checksum(校验和): 检测UPD数据报在传输中是否有错, 有错就丢弃. 该字段可选,当源主机不想计算校验和，则直接令该字段为全0.
 * 当从IP层收到UDP数据报时，就根据首部中的目的端口，把UDP数据报通过相应的端口，上交给进程。如果接收方UDP发现收到的报文中目的端口号不正确（即不存在对应端口号的应用进程），就丢弃该报文，并由ICMP发送“端口不可达”差错报文交给发送方。
 * */
class UDPHeader(data: ByteArray, offset: Int = 0) : ProtocolHeader(data, offset) {
    companion object{
        const val SourcePort = "SourcePort"
        const val DestPort = "DestPort"
        const val DataLength = "DataLength"
        const val Checksum = "Checksum"
    }

    //有多个bits且不能被8整除时，需提供bitMask，bits小于0时，需提供dynamicBits
    override val fields = listOf(
        f(SourcePort,  16,  "源(Source Port)端口号字段：占2字节（16bit）"),
        f(DestPort,  16, "目标(Destination Port)端口号字段：占2字节（16bit）"),
        f(DataLength, 16,  "长度包含首部和数据, 最小值为8字节,即只有头部的8bytes"),
        f(Checksum, 16,  "当从IP层收到UDP数据报时，就根据首部中的目的端口，把UDP数据报通过相应的端口，上交给进程。如果接收方UDP发现收到的报文中目的端口号不正确（即不存在对应端口号的应用进程），就丢弃该报文，并由ICMP发送“端口不可达”差错报文交给发送方。"),
    )
}