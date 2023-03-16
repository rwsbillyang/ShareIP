/*
 * Copyright © 2023 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2023-02-09 18:45
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

/**
 * @param name NAME	不定长	资源记录包含的域名。
 * @param type TYPE	2个字节	表示资源记录的类型，指出RDATA数据的含义。
 * @param clas CLASS	2个字节	表示RDATA的类。
 * @param ttl TTL	4字节	无符号整数，表示资源记录可以缓存的时间。0代表只能被传输，但是不能被缓存。
 * @param data RDATA	不定长	字符串，表示记录，格式跟TYPE和CLASS有关。比如，TYPE是A，CLASS是IN，那么RDATA就是一个4个字节的ARPA网络地址。
 * @param dataLength RDLENGTH	2个字节	无符号整数，表示RDATA的长度。
 * @param length Resource所占字节长度
 * */
class Resource(val name: String, val type: Short, val clas: Short, val ttl: Int,
               val data: ByteArray) {
    var offset = -1 //所处buffer位置
    var dataLength: Short = -1 //data字节长度
    var length = -1 //Resource所占字节长度
    companion object{
        /**
         * 从ByteBuffer中解析出Resource的各字段值，然后返回Resource
         * */
        fun fromBytes(buffer: ByteBuffer): Resource {
            val offset = buffer.arrayOffset() + buffer.position()
            val domain = DNSPacket.readDomain(buffer, buffer.arrayOffset())
            val type = buffer.short
            val clas = buffer.short
            val ttl  = buffer.int
            val dataLength = buffer.short
            val data = ByteArray(dataLength.toInt() and 0xFFFF)
            buffer.get(data)
            val length = buffer.arrayOffset() + buffer.position() - offset
            return Resource(domain, type, clas, ttl, data).also {
                it.offset = offset
                it.dataLength = dataLength
                it.length = length
            }
        }
    }

    /**
     * 将Resource字段值写入ByteBuffer
     * */
    fun toBytes(buffer: ByteBuffer) {
        val len = data.size.toShort()

        offset = buffer.position()
        DNSPacket.writeDomain(name, buffer)
        buffer.putShort(type)
        buffer.putShort(clas)
        buffer.putInt(ttl)
        buffer.putShort(len)
        buffer.put(data)

        dataLength = len
        length = buffer.position() - offset
    }
}