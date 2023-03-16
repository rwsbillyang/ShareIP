/*
 * Copyright © 2023 rwsbillyang@qq.com 
 *  
 * Written by rwsbillyang@qq.com at Beijing Time: 2023-02-09 12:06 
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

package com.github.rwsbillyang.proxy.protocol.dns

import com.github.rwsbillyang.proxy.protocol.ProtocolHeader


/**
 *    MSB------------------------>LSB
0  1  2  3  4  5  6  7  0  1  2  3  4  5  6  7
+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
|                      ID                       |
+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
|QR|  opcode   |AA|TC|RD|RA|   Z    |   RCODE   |
+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
|                    QDCOUNT                    |
+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
|                    ANCOUNT                    |
+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
|                    NSCOUNT                    |
+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
|                    ARCOUNT                    |
+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 * 共12字节
 * ID：第1行，2字节（16位），请求者生成，DNS服务器在响应时会使用该ID，这样便于请求程序区分不同的DNS响应。
 * 第2行：
 * QR：占1位。指示该消息是请求还是响应。0表示请求；1表示响应。
 * OPCODE：占4位。指示请求的类型，有请求发起者设定，响应消息中复用该值。0表示标准查询；1表示反转查询；2表示服务器状态查询。3~15目前保留，以备将来使用。
 * AA（Authoritative Answer，权威应答）：占1位。表示响应的服务器是否是权威DNS服务器。只在响应消息中有效。
 * TC（TrunCation，截断）：占1位。指示消息是否因为传输大小限制而被截断。
 * RD（Recursion Desired，期望递归）：占1位。该值在请求消息中被设置，响应消息复用该值。如果被设置，表示希望服务器递归查询。但服务器不一定支持递归查询。
 * RA（Recursion Available，递归可用性）：占1位。该值在响应消息中被设置或被清除，以表明服务器是否支持递归查询。
 * Z：占3位。保留备用。
 * RCODE（Response code）：占4位。该值在响应消息中被设置。取值及含义如下：
 * 0：No error condition，没有错误条件；
 * 1：Format error，请求格式有误，服务器无法解析请求；
 * 2：Server failure，服务器出错。
 * 3：Name Error，只在权威DNS服务器的响应中有意义，表示请求中的域名不存在。
 * 4：Not Implemented，服务器不支持该请求类型。
 * 5：Refused，服务器拒绝执行请求操作。
 * 6~15：保留备用。
 *
 * 第3行：QDCOUNT：占16位（无符号）。指明Question部分的包含的实体数量。
 * 第4行：ANCOUNT：占16位（无符号）。指明Answer部分的包含的RR（Resource Record）数量。
 * 第5行：NSCOUNT：占16位（无符号）。指明Authority部分的包含的RR（Resource Record）数量。
 * 第6行：ARCOUNT：占16位（无符号）。指明Additional部分的包含的RR（Resource Record）数量。
 * */
class DNSHeader(data: ByteArray, offset: Int = 0) : ProtocolHeader(data, offset) {
    companion object{
        const val ID = "ID"
        const val QR = "QR"
        const val OPCODE = "OPCODE"
        const val Flag_AA = "Flag_URG"
        const val Flag_TC = "Flag_ACK"
        const val Flag_RD = "Flag_PSH"
        const val Flag_RA = "Flag_RST"
        const val Flag_Z = "Flag_SYN"
        const val RCODE = "Flag_FIN"
        const val Question_Count = "QDCOUNT"
        const val Answer_Count = "ANCOUNT"
        const val Authority_Count = "NSCOUNT"
        const val Additional_Count = "ARCOUNT"

        val fixedLength = 12
    }

    //有多个bits且不能被8整除时，需提供bitMask，bits小于0时，需提供dynamicBits
    override val fields = listOf(
        f(ID,  16,  "2字节（16位），请求者生成，DNS服务器在响应时会使用该ID，这样便于请求程序区分不同的DNS响应。"),
        f(QR,  1, "QR：占1位。指示该消息是请求还是响应。0表示请求；1表示响应。"),
        f(OPCODE,  4,  "占4位。指示请求的类型，有请求发起者设定，响应消息中复用该值。0表示标准查询；1表示反转查询；2表示服务器状态查询。3~15目前保留，以备将来使用。"),
        f(Flag_AA, 1,  "AA（Authoritative Answer，权威应答）：占1位。表示响应的服务器是否是权威DNS服务器。只在响应消息中有效。"),
        f(Flag_TC, 1,  "TC（TrunCation，截断）：占1位。指示消息是否因为传输大小限制而被截断。"),
        f(Flag_RD, 1,  "RD（Recursion Desired，期望递归）：占1位。该值在请求消息中被设置，响应消息复用该值。如果被设置，表示希望服务器递归查询。但服务器不一定支持递归查询。"),
        f(Flag_RA, 1,  "RA（Recursion Available，递归可用性）：占1位。该值在响应消息中被设置或被清除，以表明服务器是否支持递归查询。"),
        f(Flag_Z, 3,  "Z：占3位。保留备用。"),
        f(RCODE,  4,  "RCODE（Response code）：占4位。该值在响应消息中被设置。取值及含义如下：0：No error condition，没有错误条件；1：Format error，请求格式有误，服务器无法解析请求； 2：Server failure，服务器出错。3：Name Error，只在权威DNS服务器的响应中有意义，表示请求中的域名不存在。4：Not Implemented，服务器不支持该请求类型。5：Refused，服务器拒绝执行请求操作。6~15：保留备用。"),
        f(Question_Count, 16,  "QDCOUNT：占16位（无符号）。指明Question部分的包含的实体数量。"),
        f(Answer_Count, 16,  "ANCOUNT：占16位（无符号）。指明Answer部分的包含的RR（Resource Record）数量。"),
        f(Authority_Count, 16,  "NSCOUNT：占16位（无符号）。指明Authority部分的包含的RR（Resource Record）数量。"),
        f(Additional_Count, 16,  "ARCOUNT：占16位（无符号）。指明Additional部分的包含的RR（Resource Record）数量。"),
    )
}