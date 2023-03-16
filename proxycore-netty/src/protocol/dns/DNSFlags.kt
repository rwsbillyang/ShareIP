/*
 * Copyright © 2023 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2023-02-09 17:48
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

/**
 * |QR|  opcode   |AA|TC|RD|RA|   Z    |   RCODE   |
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
 * */
class DNSFlags(val QR: Boolean, val OpCode: Int, val AA: Boolean, val TC: Boolean,
               val RD: Boolean, val RA: Boolean, val Zero: Int, val Rcode: Int ) {

    companion object{
        fun parse(value: Short): DNSFlags {
            val flags = value.toInt() and 0xFFFF

            val QR = ((flags shr 7) and 0x01) == 1
            val OpCode = (flags shr 3) and 0x0F
            val AA = ((flags shr 2) and 0x01) == 1
            val TC = ((flags shr 1) and 0x01) == 1
            val RD = (flags and 0x01) == 1
            val RA = (flags shr 15) == 1
            val Zero = (flags shr 12) and 0x07
            val Rcode = (flags shr 8) and 0xF

            return DNSFlags(QR, OpCode, AA, TC, RD, RA, Zero,Rcode)
        }
    }

    fun toShort(): Short {
        var flag = 0
        flag = flag or ((if (QR) 1 else 0) shl 7)
        flag = flag or (OpCode and 0x0F shl 3)
        flag = flag or ((if (AA) 1 else 0) shl 2)
        flag = flag or ((if (TC) 1 else 0) shl 1)
        flag = flag or if (RD) 1 else 0
        flag = flag or ((if (RA) 1 else 0) shl 15)
        flag = flag or (Zero and 0x07 shl 12)
        flag = flag or (Rcode and 0x0F shl 8)
        return flag.toShort()
    }
}