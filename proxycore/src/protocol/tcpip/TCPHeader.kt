/*
 * Copyright © 2023 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2023-02-14 12:17
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
 * TCP确保了可靠传输，流量控制、拥塞控制等
 * TCP Header Format
 * 0                   1                   2                   3
 * 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |          Source Port          |       Destination Port        |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                        Sequence Number                        |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                    Acknowledgment Number                      |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |  Data |           |U|A|P|R|S|F|                               |
 * | Offset| Reserved  |R|C|S|S|Y|I|            Window             |
 * |       |           |G|K|H|T|N|N|                               |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |           Checksum            |         Urgent Pointer        |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                    Options                    |    Padding    |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                             data                              |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * 第一行，源(Source Port)、目标(Destination Port)端口号字段：各占2字节（16bit）.
 * TCP协议通过使用"端口"来标识源端和目标端的应用进程。端口号可以使用0到65535之间的任何数字。
 *
 * 第二行，顺序号字段(Sequence Number): 4字节（32bit）.
 *
 * 第三行，确认号字段(Acknowledgment Number): 32bit. 只有ACK标志为1时,该字段才有效.
 * 接收方收到发送方一个的 TCP 包之后，取出其中的 sequence number，在下一个接收方自己要发送的包中，设置 ack 比特位为 1，同时设置 acknowledge number 为 sequence number + 1。
 *
 * 第四行 头部长度字段(Data Offset): 4bit. 标示头部长度, 即头部占多少个32bit. 没有选项字段时,该字段值为20字节(5 * 32bit = 160bit).
 * 最长可以有60字节(15 * 32bit = 480bit)
 * 保留字段(Reserved): 6bit. (Reserved 3bit, Nonc 1bit, Congestion Window Reduced (CWR) 1bit, ECN-Echo 1bit)
 * 标志位字段(U、A、P、R、S、F): 占6bit
 * URG: 紧急指针字段有效
 * ACK: 确认序号有效
 * PSH: 接收方应该尽快将这个报文交给应用层(数据传输)
 * RST: 重建连接
 * SYN: 发起一个连接
 * FIN: 释放一个连接
 * 窗口大小(Window): 16bit. 此字段用来进行流量控制。单位为字节数，这个值是本机期望一次接收的字节数。
 *
 * 第五行，TCP校验和字段(Checksum): 16bit. 对整个TCP报文段，即TCP头部和TCP数据进行校验和计算，并由目标端进行验证。
 * 紧急指针字段(Urgent Pointer)：占16比特. 它是一个偏移量，和序号字段中的值相加表示紧急数据最后一个字节的序号
 *
 * 第6行，选项字段：占32比特。可能包括"窗口扩大因子"、"时间戳"等选项。
 * */
class TCPHeader(data: ByteArray, offset: Int = 0) : ProtocolHeader(data, offset) {
    companion object{
        const val SourcePort = "SourcePort"
        const val DestPort = "DestPort"
        const val Seq = "Seq"
        const val Ack = "Ack"
        const val HeaderLength = "HeaderLength"
        const val Flag_reserve = "Flag_reserve"
        const val Flag_URG = "Flag_URG"
        const val Flag_ACK = "Flag_ACK"
        const val Flag_PSH = "Flag_PSH"
        const val Flag_RST = "Flag_RST"
        const val Flag_SYN = "Flag_SYN"
        const val Flag_FIN = "Flag_FIN"
        const val Window = "Window"
        const val Checksum = "Checksum"
        const val UrgentPointer = "UrgentPointer"
        const val Options = "Options"
    }

    //有多个bits且不能被8整除时，需提供bitMask，bits小于0时，需提供dynamicBits
    override val fields = listOf(
        f(SourcePort,  16,  "源(Source Port)端口号字段：占2字节（16bit）"),
        f(DestPort,  16, "目标(Destination Port)端口号字段：占2字节（16bit）"),
        f(Seq,  32,  "顺序号字段(Sequence Number): 4字节（32bit）"),
        f(Ack, 32,  "确认号字段(Acknowledgment Number): 32bit. 只有ACK标志为1时,该字段才有效"),
        f(HeaderLength, 4,  "头部长度字段(Data Offset): 4bit. 标示头部长度, 即头部占多少个32bit. 没有选项字段时,该字段值为20字节(5 * 32bit = 160bit) 最长可以有60字节(15 * 32bit = 480bit)"),
        f(Flag_reserve, 6,  "保留字段(Reserved): 6bit. (Reserved 3bit, Nonc 1bit, Congestion Window Reduced (CWR) 1bit, ECN-Echo 1bit)"),
        f(Flag_URG, 1,  "紧急指针字段有效"),
        f(Flag_ACK, 1,  "确认序号有效"),
        f(Flag_PSH,  1,  "接收方应该尽快将这个报文交给应用层(数据传输)"),
        f(Flag_RST, 1,  "重建连接"),
        f(Flag_SYN,  1, "发起一个连接"),
        f(Flag_FIN, 1,  "释放一个连接"),
        f(Window, 16,  "窗口大小(Window): 16bit. 此字段用来进行流量控制。单位为字节数，这个值是本机期望一次接收的字节数"),
        f(Checksum, 16,  "TCP校验和字段(Checksum): 16bit. 对整个TCP报文段，即TCP头部和TCP数据进行校验和计算，并由目标端进行验证"),
        f(UrgentPointer, 16,  "紧急指针字段(Urgent Pointer)：占16比特. 它是一个偏移量，和序号字段中的值相加表示紧急数据最后一个字节的序号"),
        f(Options, -1,  "选项字段：32比特的整数倍。可能包括窗口扩大因子、时间戳等选项"){
            data.size - 160
        },
    )
}