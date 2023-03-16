/*
 * Copyright © 2023 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2023-02-13 22:29
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
 * IP层只管把数据包送到远程目的地IP，中间出错会由路由器返回ICMP包，把错误信息封包，然后传送回给主机
 * IP层依赖数据链路层在物理节点之间的传输
 *
 * https://nmap.org/book/tcpip-ref.html
 *
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
class IPHeader(data: ByteArray, offset: Int = 0) : ProtocolHeader(data, offset) {
    companion object{
        const val Version = "Version"
        const val HeaderLength = "HeaderLength"
        const val TypeOfService = "TypeOfService"
        const val TotalLength = "totalLength"
        const val ID = "ID"
        const val Flag_reserve = "Flag_reserve"
        const val Flag_not_fragment = "Flag_not_fragment"
        const val Flag_more_fragment = "Flag_more_fragment"
        const val FragmentOffset = "FragmentOffset"
        const val TTL = "TTL"
        const val Protocol = "Protocol"
        const val Checksums = "Checksums"
        const val SrcIP = "SrcIP"
        const val DestIP = "DestIP"
        const val Options = "Options"

        /**
         * Internet protocol version 4.
         */
        const val Version_IPV4 = 4

        /**
         * Internet protocol version 6.
         */
        const val Version_IPV6 = 6
    }

    override val fields = listOf(
        f(Version,  4,  "Version(版本): 4bit, 表示IP协议实现的版本号,IPV4即0100"),
        f(HeaderLength,  4, "IP header长度: 4bit, 表示头部的长度, 有多少个32比特.如果不包含Options(选项字段), 则长度为(5 * 32bit = 160bit)20字节, 最长为(15 * 32bit = 480bit)60字节"),
        f(TypeOfService,  8,"Type of Service(服务类型): 8bit, 其中前3bit为优先权字段,现已弃用. 第8bit保留.第4至7字段分别表示延迟、吞吐量、可靠性、花费.当它们取值为1时分别表示要求最小延迟、最大吞吐量、最高可靠性、最小费用.同时只能其中之一为1. 可以全为0表示一般服务. 服务类型声明了数据被网络系统传输时可以被怎样处理."),
        f(TotalLength, 16,"Total Length(总长度字段): 16bit. 表示整个数据报的长度, 最大长度65535字节."),
        f(ID, 16,"Identification(标示): 16bit. 用来唯一地标识主机发送的每一份数据报。通常每发一份报文，它的值会加1。"),
        f(Flag_reserve, 1,"Flags(标记): 3bit 第1位不使用"),
        f(Flag_not_fragment, 1,"Flags(标记): 第2位DF(Don't Fragment)设为1时表示不能对该上层数据包分片,如果不分片无法转发,路由器会丢弃该数据返回一个错误"),
        f(Flag_more_fragment, 1,"Flags(标记): 3bit 第3位是MF(More Fragment),当路由器对上层数据包分段,则路由器会在除最后一个分段外所有分段该位置为1"),
        f(FragmentOffset,  13,"Fragment Offset(片偏移): 13bit. 在对上层数据包分段后, 该字段表示当前IP包在其中的序列. 保证了目标路由器在接收到IP包后能还原上层数据. 若其中一个分段丢失,则包含该分段的整个上层数据包都会要求重传."),
        f(TTL, 8,"TTL(生存期): 8bit,用来表示该数据报文最多可以经过的路由器数，没经过一个路由器都减1，直到为0数据包丢掉。这个字段可以防止由于故障而导致IP包在网络中不停被转发"),
        f(Protocol,  8,"Protocol(协议字段): 8bit. 标示IP层所封装的上层协议类型, 如传输层TCP/UDP/ICMP/IGMP"),
        f(Checksums, 16,"Header checksum(头部校验和字段): 16bit. 内容是根据IP头部计算得到的校验和码. 计算方法是:对头部中每个16bit进行二进制反码求和. (和ICMP、IGMP、TCP、UDP不同，IP不对头部后的数据进行校验).checksum字段置为0，对Header中每16bit做为一个值进行相加，相加的进位加到低16位上，对结果取反，赋值时,转换为网络字节序"),
        f(SrcIP, 32,"源地址，占32bit(IPV4)"),
        f(DestIP, 32,"目的地址. 占32bit(IPV4)"),
        f(Options, -1,"Option(选项字段): 必须是32比特的整数倍，如果不足，必须填充0, 最长15*32bit. 用来定义一些任选项：如记录路径、时间戳等。这些选项很少被使用，同时并不是所有主机和路由器都支持这些选项"){
            var ihl = getInt(HeaderLength)?:5
            if(ihl < 5) {
                log.warn("HeaderLength < 5")
                ihl = 5
            }
            (ihl*4 - 20) * 8
        },
    )
}
object IPProtocol{

    /**
     * Dummy protocol for TCP.
     */
    const val Protocol_IP = 0

    /**
     * IPv6 Hop-by-Hop options.
     */
    const val Protocol_HOPOPTS = 0

    /**
     * Internet Control Message Protocol.
     */
    const val Protocol_ICMP = 1

    /**
     * Internet Group Management Protocol.
     */
    const val Protocol_IGMP = 2

    /**
     * IPIP tunnels (older KA9Q tunnels use 94).
     */
    const val Protocol_IPIP = 4

    /**
     * Transmission Control Protocol.
     */
    const val Protocol_TCP = 6

    /**
     * Exterior Gateway Protocol.
     */
    const val Protocol_EGP = 8

    const val Protocol_IGRP = 9
    /**
     * PUP protocol.
     */
    const val Protocol_PUP = 12

    /**
     * User Datagram Protocol.
     */
    const val Protocol_UDP = 17

    /**
     * XNS IDP protocol.
     */
    const val Protocol_IDP = 22

    /**
     * SO Transport Protocol Class 4.
     */
    const val Protocol_TP = 29

    /**
     * IPv6 header.
     */
    const val Protocol_IPV6 = 41

    /**
     * IPv6 routing header.
     */
    const val Protocol_ROUTING = 43

    /**
     * IPv6 fragmentation header.
     */
    const val Protocol_FRAGMENT = 44

    /**
     * Reservation Protocol.
     */
    const val Protocol_RSVP = 46

    /**
     * General Routing Encapsulation.
     */
    const val Protocol_GRE = 47

    /**
     * encapsulating security payload.
     */
    const val Protocol_ESP = 50

    /**
     * authentication header.
     */
    const val Protocol_AH = 51

    const val Protocol_SKIP = 57
    /**
     * ICMPv6.
     */
    const val Protocol_ICMPV6 = 58

    /**
     * IPv6 no next header.
     */
    const val Protocol_NONE = 59

    /**
     * IPv6 destination options.
     */
    const val Protocol_DSTOPTS = 60

    const val Protocol_EIGRP = 88
    const val Protocol_OSPF = 89
    /**
     * Multicast Transport Protocol.
     */
    const val Protocol_MTP = 92

    /**
     * Encapsulation Header.
     */
    const val Protocol_ENCAP = 98

    /**
     * Protocol Independent Multicast.
     */
    const val Protocol_PIM = 103

    /**
     * Compression Header Protocol.
     */
    const val Protocol_COMP = 108


    const val Protocol_L2TP = 115

    /**
     * Raw IP packets.
     */
    const val Protocol_RAW = 255


    /**
     * Unrecognized IP protocol.
     * WARNING: this only works because the int storage for the protocol
     * code has more bits than the field in the IP header where it is stored.
     */
    const val Protocol_INVALID = -1
}