/*
 * Copyright © 2023 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2023-02-09 12:03
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

import com.github.rwsbillyang.proxy.protocol.HeaderUtil
import java.nio.ByteBuffer

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
class TCPHeader(val data: ByteArray, val offset: Int= 0) {
    companion object{
        const val MIN_BYTES = 20
        const val MAX_BYTES = 60

        const val FIN = 1
        const val SYN = 2
        const val RST = 4
        const val PSH = 8
        const val ACK = 16
        const val URG = 32

        const val offset_src_port: Short = 0 // 16位源端口

        const val offset_dest_port: Short = 2 // 16位目的端口

        const val offset_seq = 4 // 32位序列号

        const val offset_ack = 8 // 32位确认号

        const val offset_lenres: Byte = 12 // 4位首部长度/4位保留字

        const val offset_flag: Byte = 13 // 6位标志位

        const val offset_win: Short = 14 // 16位窗口大小

        const val offset_crc: Short = 16 // 16位校验和

        const val offset_urp: Short = 18 // 16位紧急数据偏移量

        fun fromBytes(buffer: ByteBuffer){
            if(buffer.remaining() < MIN_BYTES){

            }
        }
    }


    fun getHeaderLength(): Int {
        val len: Int = data.get(offset + offset_lenres).toInt() and 0xFF
        return (len shr 4) * 4
    }
    fun getSourcePort(): Short {
        return HeaderUtil.readShort(data, offset + offset_src_port)
    }

    fun getDestPort(): Short {
        return HeaderUtil.readShort(data, offset + offset_dest_port)
    }
    fun getSeqId(): Int {
        return HeaderUtil.readInt(data, offset + offset_seq)
    }
    fun getAckId(): Int {
        return HeaderUtil.readInt(data, offset + offset_ack)
    }

    fun getFlags(): Byte {
        return data.get(offset + offset_flag)
    }

    fun getCrc(): Short {
        return HeaderUtil.readShort(data, offset + offset_crc)
    }


    fun setSourcePort(value: Short) {
        HeaderUtil.writeShort(data, offset + offset_src_port, value)
    }

    fun setDestPort(value: Short) {
        HeaderUtil.writeShort(data, offset + offset_dest_port, value)
    }

    fun setCrc(value: Short) {
        HeaderUtil.writeShort(data, offset + offset_crc, value)
    }

    override fun toString(): String {
        val flags = getFlags().toInt()
        return String.format(
            "%s%s%s%s%s%s%d->%d %s:%s",
            if ((flags and SYN) == SYN) "SYN " else "",
            if ((flags and ACK) == ACK) "ACK " else "",
            if ((flags and PSH) == PSH) "PSH " else "",
            if ((flags and RST) == RST) "RST " else "",
            if ((flags and FIN) == FIN) "FIN " else "",
            if ((flags and URG) == URG) "URG " else "",
            getSourcePort().toInt() and 0xFFFF,
            getDestPort().toInt() and 0xFFFF,
            getSeqId(),
            getAckId()
        )
    }
}