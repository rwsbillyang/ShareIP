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

import com.github.rwsbillyang.proxy.protocol.HeaderUtil
import java.nio.ByteBuffer

/**
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
class DNSHeader(val id: Short,
                val flags: DNSFlags,
                val questionCount: Short,
                val anwserCount: Short,
                val authorityCount: Short,
                val addtionalCount: Short) {
    //var data: ByteArray = ByteArray(12)
    //val offset: Int= -1
    companion object{
        const val offset_ID: Short = 0
        const val offset_Flags: Short = 2
        const val offset_QuestionCount: Short = 4
        const val offset_ResourceCount: Short = 6
        const val offset_AResourceCount: Short = 8
        const val offset_EResourceCount: Short = 10

        /**
         * 从ByteBuffer中解析出DNSHeader的各字段值，然后返回DNSHeader
         * */
        fun fromBytes(buffer: ByteBuffer):DNSHeader {
            val offset = buffer.arrayOffset() + buffer.position()
            val id = buffer.short
            val flags = DNSFlags.parse(buffer.short)
            val questionCount = buffer.short
            val anwserCount = buffer.short
            val authorityCount = buffer.short
            val addtionalCount = buffer.short
            return DNSHeader(id, flags, questionCount, anwserCount, authorityCount, addtionalCount)
        }
    }
    /**
     * 将DNSHeader字段值写入ByteBuffer
     * */
    fun toBytes(buffer: ByteBuffer) {
        buffer.putShort(id)
        buffer.putShort(flags.toShort())
        buffer.putShort(questionCount)
        buffer.putShort(anwserCount)
        buffer.putShort(authorityCount)
        buffer.putShort(addtionalCount)
    }

//    fun getID(): Short {
//        return HeaderUtil.readShort(data, offset + offset_ID)
//    }
//
//    fun getFlags(): DNSFlags {
//        val v = HeaderUtil.readShort(data, offset + offset_Flags)
//        return DNSFlags.parse(v)
//    }
//
//    /**
//     * Question部分的包含的实体数量
//     * */
//    fun getQuestionCount(): Short {
//        return HeaderUtil.readShort(data, offset + offset_QuestionCount)
//    }
//    /**
//     * Answer部分的包含的RR（Resource Record）数量
//     * */
//    fun getResourceCount(): Short {
//        return HeaderUtil.readShort(data, offset + offset_ResourceCount)
//    }
//
//    /**
//     * Authority部分的包含的RR（Resource Record）数量
//     * */
//    fun getAResourceCount(): Short {
//        return HeaderUtil.readShort(data, offset + offset_AResourceCount)
//    }
//    /**
//     * Additional部分的包含的RR（Resource Record）数量
//     * */
//    fun getEResourceCount(): Short {
//        return HeaderUtil.readShort(data, offset + offset_EResourceCount)
//    }
//
//    fun setID(value: Short) {
//        HeaderUtil.writeShort(data, offset + offset_ID, value)
//    }
//
//    fun setFlags(value: Short) {
//        HeaderUtil.writeShort(data, offset + offset_Flags, value)
//    }
//
//    fun setQuestionCount(value: Short) {
//        HeaderUtil.writeShort(data, offset + offset_QuestionCount, value)
//    }
//
//    fun setResourceCount(value: Short) {
//        HeaderUtil.writeShort(data, offset + offset_ResourceCount, value)
//    }
//
//    fun setAResourceCount(value: Short) {
//        HeaderUtil.writeShort(data, offset + offset_AResourceCount, value)
//    }
//
//    fun setEResourceCount(value: Short) {
//        HeaderUtil.writeShort(data, offset + offset_EResourceCount, value)
//    }


}