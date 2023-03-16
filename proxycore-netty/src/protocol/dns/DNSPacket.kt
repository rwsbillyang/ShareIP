/*
 * Copyright © 2023 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2023-02-09 18:46
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

import java.nio.ByteBuffer

class DNSPacket(val header: DNSHeader,
                val questions: List<Question>,
                val resources: List<Resource>,
                val AResources: List<Resource>,
                val EResources: List<Resource>
) {
    var size = -1
    companion object{
        fun fromBytes(buffer: ByteBuffer): DNSPacket? {
            if (buffer.limit() < 12) return null
            if (buffer.limit() > 512) return null
            val header = DNSHeader.fromBytes(buffer)

            if (header.questionCount > 2 || header.anwserCount > 50 || header.authorityCount > 50 || header.addtionalCount > 50) {
                println("invalid count: header.questionCount=${header.questionCount}, or header.anwserCount=${header.anwserCount}, or header.authorityCount=${header.authorityCount}, or header.addtionalCount=${header.addtionalCount}")
                return null
            }

            val size=buffer.limit()
            val questions = mutableListOf<Question>()
            val resources = mutableListOf<Resource>()
            val AResources = mutableListOf<Resource>()
            val EResources = mutableListOf<Resource>()

            (0 until header.questionCount).forEach {
                questions[it] = Question.fromBytes(buffer)
            }
            (0 until header.anwserCount).forEach {
                resources[it] = Resource.fromBytes(buffer)
            }
            (0 until header.authorityCount).forEach {
                AResources[it] = Resource.fromBytes(buffer)
            }
            (0 until header.addtionalCount).forEach  {
                EResources[it] = Resource.fromBytes(buffer)
            }
            return DNSPacket(header, questions, resources, AResources, EResources).also {
                it.size = size
            }
        }

        /**
         * QNAME：字节数不定，以0x00作为结束符。表示查询的主机名。注意：众所周知，主机名被"."号分割成了多段标签。
         * 在QNAME中，每段标签前面加一个数字，表示接下来标签的长度。比如：api.sina.com.cn表示成QNAME时，
         * 会在"api"前面加上一个字节0x03，"sina"前面加上一个字节0x04，"com"前面加上一个字节0x03，而"cn"前面加上一个字节0x02
         * */
        fun readDomain(buffer: ByteBuffer, dnsHeaderOffset: Int): String {
            val sb = StringBuilder()
            var len = 0
            while (buffer.hasRemaining() && (buffer.get().toInt() and 0xFF).also { len = it } > 0) {
                if (len and 0xc0 == 0xc0) //
                {
                    var pointer = buffer.get().toInt() and 0xFF
                    pointer = pointer or ((len and 0x3F) shl 8)
                    val newBuffer =
                        ByteBuffer.wrap(buffer.array(), dnsHeaderOffset + pointer, dnsHeaderOffset + buffer.limit())
                    sb.append(readDomain(newBuffer, dnsHeaderOffset))
                    return sb.toString()
                } else {
                    while (len > 0 && buffer.hasRemaining()) {
                        sb.append((buffer.get().toInt() and 0xFF).toChar())
                        len--
                    }
                    sb.append('.')
                }
            }
            if (len == 0 && sb.isNotEmpty()) {
                sb.deleteCharAt(sb.length - 1)
            }
            return sb.toString()
        }

        fun writeDomain(domain: String?, buffer: ByteBuffer) {
            if (domain == null || domain === "") {
                buffer.put(0.toByte())
                return
            }
            val arr = domain.split(".").dropLastWhile { it.isEmpty() }.toTypedArray()
            for (item in arr) {
                if (arr.size > 1) {
                    buffer.put(item.length.toByte())
                }
                for (i in item.indices) {
                    buffer.put(item.codePointAt(i).toByte())
                }
            }
        }
    }


    fun toBytes(buffer: ByteBuffer){
        header.toBytes(buffer)

        (0 until header.questionCount).forEach {
            questions[it].toBytes(buffer)
        }
        (0 until header.anwserCount).forEach {
            resources[it].toBytes(buffer)
        }
        (0 until header.authorityCount).forEach {
            AResources[it].toBytes(buffer)
        }
        (0 until header.addtionalCount).forEach  {
            EResources[it].toBytes(buffer)
        }
    }
}