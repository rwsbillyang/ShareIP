/*
 * Copyright © 2023 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2023-02-09 18:44
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
 * @param name QNAME：字节数不定，以0x00作为结束符。表示查询的主机名。注意：众所周知，主机名被"."号分割成了多段标签。
 * 在QNAME中，每段标签前面加一个数字，表示接下来标签的长度。比如：api.sina.com.cn表示成QNAME时，
 * 会在"api"前面加上一个字节0x03，"sina"前面加上一个字节0x04，"com"前面加上一个字节0x03，而"cn"前面加上一个字节0x02；
 * @param type QTYPE：占2个字节。表示RR类型，见以上RR介绍；
 * @param clas QCLASS：占2个字节。表示RR分类，见以上RR介绍
 * */
class Question(val name: String, val type: Short, val clas: Short) {
    var offset = -1//所处buffer位置
    var length: Int  = -1//Question所占字节长度
    companion object{
        /**
         * 从ByteBuffer中解析出Question的各字段值，然后返回Question
         * */
        fun fromBytes(buffer: ByteBuffer): Question {
            val offset = buffer.arrayOffset() + buffer.position()
            val domain = DNSPacket.readDomain(buffer, buffer.arrayOffset())
            val type = buffer.short
            val clas = buffer.short
            val length = buffer.arrayOffset() + buffer.position() - offset
            return Question(domain, type, clas).also {
                it.offset = offset
                it.length = length
            }
        }
    }

    /**
     * 将Question字段值写入ByteBuffer
     * */
    fun toBytes(buffer: ByteBuffer) {
        offset = buffer.position()
        DNSPacket.writeDomain(name, buffer)
        buffer.putShort(type)
        buffer.putShort(clas)
        length = buffer.position() - offset
    }
}