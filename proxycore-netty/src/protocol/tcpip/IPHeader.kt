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


/**
 * IP层只管把数据包送到远程目的地IP，中间出错会由路由器返回ICMP包，把错误信息封包，然后传送回给主机
 * IP层依赖数据链路层在物理节点之间的传输
 * 0                   1                   2                   3
 * 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |Version|  IHL  |Type of Service|          Total Length         |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |         Identification        |Flags|      Fragment Offset    |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |  Time to Live |    Protocol   |         Header Checksum       |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                       Source Address                          |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                    Destination Address                        |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                    Options                    |    Padding    |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *
 * Version(版本): 4bit, 表示IP协议实现的版本号,当前是IPV4, 即0100
 * IHL(报头长度): 4bit, 表示头部的长度, 有多少个32比特.如果不包含Options(选项字段), 则长度为(5 * 32bit = 160bit)20字节, 最长为(15 * 32bit = 480bit)60字节
 * Type of Service(服务类型): 8bit, 其中前3bit为优先权字段,现已弃用. 第8bit保留.第4至7字段分别表示延迟、吞吐量、可靠性、花费.当它们取值为1时分别表示要求最小延迟、最大吞吐量、最高可靠性、最小费用.
 * 同时只能其中之一为1. 可以全为0表示一般服务. 服务类型声明了数据被网络系统传输时可以被怎样处理.
 * Total Length(总长度字段): 16bit. 表示整个数据报的长度, 最大长度65535字节.
 * 
 * Identification(标示): 16bit. 用来唯一地标识主机发送的每一份数据报。通常每发一份报文，它的值会加1。
 * Flags(标记): 3bit 第一位不使用 第二位DF(Don't Fragment)设为1时表示不能对该上层数据包分片,如果不分片无法转发,路由器会丢弃该数据返回一个错误. 
 * 第三位是MF(More Fragment),当路由器对上层数据包分段,则路由器会在除最后一个分段外所有分段该位置为1
 *
 * Fragment Offset(片偏移): 13bit. 在对上层数据包分段后, 该字段表示当前IP包在其中的序列. 保证了目标路由器在接收到IP包后能还原上层数据. 若其中一个分段丢失,则包含该分段的整个上层数据包都会要求重传.
 * 
 * TTL(生存期): 8bit,用来表示该数据报文最多可以经过的路由器数，没经过一个路由器都减1，直到为0数据包丢掉。这个字段可以防止由于故障而导致IP包在网络中不停被转发。 
 * Protocol(协议字段): 8bit. 标示IP层所封装的上层协议类型, 如传输层TCP/UDP/ICMP/IGMP
 * Header checksum(头部校验和字段): 16bit. 内容是根据IP头部计算得到的校验和码. 计算方法是:对头部中每个16bit进行二进制反码求和. (和ICMP、IGMP、TCP、UDP不同，IP不对头部后的数据进行校验).
 * 将checksum字段置为0，对Header中每16bit做为一个值进行相加，相加的进位加到低16位上，对结果取反，赋值时,转换为网络字节序
 * 
 * Source Address & Dest Address: 源地址和目的地址. 各占32字节(IPV4).
 * A类IP地址: 0.0.0.0~127.255.255.255    IP 地址的前 8 位代表网络 ID ，后 24 位代表主机 ＩＤ。
 * B类IP地址:128.0.0.0~191.255.255.255   IP 地址的前 16 位代表网络 ID ，后 16 位代表主机 ＩＤ。
 * C类IP地址:192.0.0.0~239.255.255.255
 * 全是 0 的主机 ID 代表网络本身，比如说 IP 地址为 130.100.0.0 指的是网络 ID 为130.100 的 B 类地址。
 * 全是 1 的主机 ID 代表广播，是用于向该网络中的全部主机方法消息的。 IP 地址为 130.100.255.255 就是网络 ID 为 130.100 网络的广播地址（二进制 IP 地址中全是 1 ，转换为十进制就是 255 ）
 *
 * Option(选项字段): 必须是32比特的整数倍，如果不足，必须填充0, 最长15*32bit. 用来定义一些任选项：如记录路径、时间戳等。这些选项很少被使用，同时并不是所有主机和路由器都支持这些选项
 * */
class IPHeader(val data: ByteArray, val offset: Int= 0) {
    companion object{
        const val IP: Short = 0x0800
        const val ICMP: Byte = 1
        const val TCP: Byte = 6
        const val UDP: Byte = 17

        const val offset_ver_ihl: Byte = 0 // 0: Version (4 bits) + Internet header length (4// bits)

        const val offset_tos: Byte = 1 // 1: Type of service

        const val offset_tlen: Short = 2 // 2: Total length

        const val offset_identification: Short = 4 // :4 Identification

        const val offset_flags_fo: Short = 6 // 6: Flags (3 bits) + Fragment offset (13 bits)

        const val offset_ttl: Byte = 8 // 8: Time to live

        const val offset_proto: Byte = 9 // 9: Protocol

        const val offset_crc: Short = 10 // 10: Header checksum

        const val offset_src_ip = 12 // 12: Source address

        const val offset_dest_ip = 16 // 16: Destination address

        const val offset_op_pad = 20 // 20: Option + Padding
    }

    /**
     * IHL(报头长度): 4bit, 表示头部的长度, 有多少个32比特.
     * 如果不包含Options(选项字段), 则长度为(5 * 32bit = 160bit)20字节, 最长为(15 * 32bit = 480bit)60字节
     * */
    fun getHeaderLength(): Int {
        return (data.get(offset + offset_ver_ihl).toInt() and 0x0F) * 4
    }
    /**
     * Total Length(总长度字段): 16bit. 表示整个数据报的长度, 最大长度65535字节.
     * */
    fun getTotalLength(): Int {
        return HeaderUtil.readShort(data, offset + offset_tlen).toInt() and 0xFFFF
    }

    fun getDataLength(): Int {
        return getTotalLength() - getHeaderLength()
    }

    /**
     * Type of Service(服务类型): 8bit, 其中前3bit为优先权字段,现已弃用. 第8bit保留.
     * 第4至7字段分别表示延迟、吞吐量、可靠性、花费.当它们取值为1时分别表示要求最小延迟、最大吞吐量、最高可靠性、最小费用.
     * 同时只能其中之一为1. 可以全为0表示一般服务. 服务类型声明了数据被网络系统传输时可以被怎样处理.
     * */
    fun getTos(): Byte {
        return data.get(offset + offset_tos)
    }


    /**
     * Idenfification(标示): 16bit. 用来唯一地标识主机发送的每一份数据报。通常每发一份报文，它的值会加1
     * */
    fun getId(): Int {
        return HeaderUtil.readShort(data, offset + offset_identification)
            .toInt() and 0xFFFF
    }


    /**
     * Flags(标记): 3bit 第一位不使用 第二位DF(Don't Fragment)设为1时表示不能对该上层数据包分片,如果不分片无法转发,路由器会丢弃该数据返回一个错误.
     * Fragment Offset(片偏移): 13bit. 在对上层数据包分段后, 该字段表示当前IP包在其中的序列. 保证了目标路由器在接收到IP包后能还原上层数据.
     * 若其中一个分段丢失,则包含该分段的整个上层数据包都会要求重传.
     * */
    fun getFlagsAndOffset(): Short {
        return HeaderUtil.readShort(data, offset + offset_flags_fo)
    }

    /**
     * TTL(生存期): 8bit,用来表示该数据报文最多可以经过的路由器数，没经过一个路由器都减1，直到为0数据包丢掉。
     * 这个字段可以防止由于故障而导致IP包在网络中不停被转发。
     * */
    fun getTTL(): Byte {
        return data.get(offset + offset_ttl)
    }

    /**
     * Protocol(协议字段): 8bit. 标示IP层所封装的上层协议类型, 如传输层TCP/UDP/ICMP/IGMP
     * */
    fun getProtocol(): Byte {
        return data.get(offset + offset_proto)
    }

    /**
     * Header checksum(头部校验和字段): 16bit. 内容是根据IP头部计算得到的校验和码.
     * 计算方法是:对头部中每个16bit进行二进制反码求和. (和ICMP、IGMP、TCP、UDP不同，IP不对头部后的数据进行校验).
     * 将checksum字段置为0，对Header中每16bit做为一个值进行相加，相加的进位加到低16位上，对结果取反，
     * 赋值时,转换为网络字节序
     *
     * */
    fun getCrc(): Short {
        return HeaderUtil.readShort(data, offset + offset_crc)
    }

    /**
     * Source Address & Dest Address: 源地址和目的地址. 各占32字节(IPV4).
     * A类IP地址: 0.0.0.0~127.255.255.255    IP 地址的前 8 位代表网络 ID ，后 24 位代表主机 ＩＤ。
     * B类IP地址:128.0.0.0~191.255.255.255   IP 地址的前 16 位代表网络 ID ，后 16 位代表主机 ＩＤ。
     * C类IP地址:192.0.0.0~239.255.255.255
     * 全是 0 的主机 ID 代表网络本身，比如说 IP 地址为 130.100.0.0 指的是网络 ID 为130.100 的 B 类地址。
     * 全是 1 的主机 ID 代表广播，是用于向该网络中的全部主机方法消息的。
     * IP 地址为 130.100.255.255 就是网络 ID 为 130.100 网络的广播地址（二进制 IP 地址中全是 1 ，转换为十进制就是 255 ）
     * */
    fun getSourceIP(): Int {
        return HeaderUtil.readInt(data, offset + offset_src_ip)
    }

    /**
     * Source Address & Dest Address: 源地址和目的地址. 各占32字节(IPV4).
     * A类IP地址: 0.0.0.0~127.255.255.255    IP 地址的前 8 位代表网络 ID ，后 24 位代表主机 ＩＤ。
     * B类IP地址:128.0.0.0~191.255.255.255   IP 地址的前 16 位代表网络 ID ，后 16 位代表主机 ＩＤ。
     * C类IP地址:192.0.0.0~239.255.255.255
     * 全是 0 的主机 ID 代表网络本身，比如说 IP 地址为 130.100.0.0 指的是网络 ID 为130.100 的 B 类地址。
     * 全是 1 的主机 ID 代表广播，是用于向该网络中的全部主机方法消息的。
     * IP 地址为 130.100.255.255 就是网络 ID 为 130.100 网络的广播地址（二进制 IP 地址中全是 1 ，转换为十进制就是 255 ）
     * */
    fun getDestIP(): Int {
        return HeaderUtil.readInt(data, offset + offset_dest_ip)
    }

    fun setDestIP(value: Int) {
        HeaderUtil.writeInt(data, offset + offset_dest_ip, value)
    }

    fun setCrc(value: Short) {
        HeaderUtil.writeShort(data, offset + offset_crc, value)
    }
    fun setSourceIP(value: Int) {
        HeaderUtil.writeInt(data, offset + offset_src_ip, value)
    }

    fun setFlagsAndOffset(value: Short) {
        HeaderUtil.writeShort(data, offset + offset_flags_fo, value)
    }
    fun setTotalLength(value: Int) {
        HeaderUtil.writeShort(data, offset + offset_tlen, value.toShort())
    }

    fun setIdentification(value: Int) {
        HeaderUtil.writeShort(data, offset + offset_identification, value.toShort())
    }
    fun setProtocol(value: Byte) {
        data[offset + offset_proto] = value
    }
    fun setTTL(value: Byte) {
        data[offset + offset_ttl] = value
    }
    fun setHeaderLength(value: Int) {
        data[offset + offset_ver_ihl] = ((4 shl 4) or (value / 4)).toByte()
    }
    fun setTos(value: Byte) {
        data[offset + offset_tos] = value
    }

    override fun toString(): String {
        return String.format(
            "%s->%s Pro=%s,HLen=%d",
            HeaderUtil.ip2String(getSourceIP()),
            HeaderUtil.ip2String(getDestIP()),
            getProtocol(),
            getHeaderLength()
        )
    }
}