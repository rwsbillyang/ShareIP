/*
 * Copyright © 2023 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2023-02-15 20:55
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

package com.github.rwsbillyang.proxy

import java.net.Inet4Address
import java.net.InetAddress
import java.net.UnknownHostException
import kotlin.experimental.inv

object HeaderUtil {
    fun ipIntToInet4Address(ip: Int): InetAddress? {
        val ipAddress = ByteArray(4)
        writeInt(ipAddress, 0, ip)
        return try {
            Inet4Address.getByAddress(ipAddress)
        } catch (e: UnknownHostException) {
            e.printStackTrace()
            null
        }
    }


    /**
     * xx.xx.xx.xx形式字符串IP转换为整数
     * */
    fun ip2Int(ip: String): Int {
        val arrStrings = ip.split(".").dropLastWhile { it.isEmpty() }
        return (arrStrings[0].toInt() shl 24
                or (arrStrings[1].toInt() shl 16)
                or (arrStrings[2].toInt() shl 8)
                or arrStrings[3].toInt())
    }

    /**
     * 从offset起始的4字节，转换为Int
     * */
    fun readInt(data: ByteArray, offset: Int): Int {
        return ((data[offset].toInt() and 0xFF) shl 24
                or ((data[offset + 1].toInt() and 0xFF) shl 16)
                or ((data[offset + 2].toInt() and 0xFF) shl 8) or ((data[offset + 3].toInt()) and 0xFF))
    }
    /**
     * 将Int数据写入offset开始的数组data中，共写入4字节
     * */
    fun writeInt(data: ByteArray, offset: Int, value: Int) {
        data[offset] = (value shr 24).toByte()
        data[offset + 1] = (value shr 16).toByte()
        data[offset + 2] = (value shr 8).toByte()
        data[offset + 3] = value.toByte()
    }
    /**
     * 从offset起始的2字节，转换为Short类型
     * */
    fun readShort(data: ByteArray, offset: Int): Short {
        val r = ((data[offset].toInt() and 0xFF) shl 8) or (data[offset + 1].toInt() and 0xFF)
        return r.toShort()
    }

    /**
     * 将short数据写入offset开始的数组data中，共写入2字节
     * */
    fun writeShort(data: ByteArray, offset: Int, value: Short) {
        data[offset] = (value.toInt() shr 8).toByte()
        data[offset + 1] = value.toByte()
    }


    fun writeByte(data: ByteArray, offset: Int, value: Byte){
        data[offset] = value
    }

    /**
     * 主机字节顺序 => 网络字节顺序
     * */
    fun h2n(u: Short): Short {
        val r = u.toInt() and 0xFFFF shl 8 or (u.toInt() and 0xFFFF shr 8)
        return r.toShort()
    }
    /**
     * 网络字节顺序 => 主机字节顺序
     * */
    fun n2h(u: Short): Short {
        val r = u.toInt() and 0xFFFF shl 8 or (u.toInt() and 0xFFFF shr 8)
        return r.toShort()
    }

    /**
     * 主机字节顺序 => 网络字节顺序
     * */
    fun h2n(u: Int): Int {
        var r = u shr 24 and 0x000000FF
        r = r or (u shr 8 and 0x0000FF00)
        r = r or (u shl 8 and 0x00FF0000)
        r = r or (u shl 24 and -0x1000000)
        return r
    }
    /**
     * 网络字节顺序 => 主机字节顺序
     * */
    fun n2h(u: Int): Int {
        var r = u shr 24 and 0x000000FF
        r = r or (u shr 8 and 0x0000FF00)
        r = r or (u shl 8 and 0x00FF0000)
        r = r or (u shl 24 and -0x1000000)
        return r
    }

    fun checksum(sum: Long, buf: ByteArray, offset: Int, len: Int): Short {
        var sum2 = sum
        sum2 += getsum(buf, offset, len)
        while (sum2 shr 16 > 0) sum2 = (sum2 and 0xFFFFL) + (sum2 shr 16)
        return sum2.toShort().inv()
    }
    fun getsum(buf: ByteArray, offset: Int, len: Int): Long {
        var offset2 = offset
        var len2 = len
        var sum: Long = 0 /* assume 32 bit long, 16 bit short */
        while (len > 1) {
            sum += (readShort(buf, offset).toInt() and 0xFFFF).toLong()
            offset2 += 2
            len2 -= 2
        }
        if (len2 > 0) /* take care of left over byte */ {
            sum += (buf[offset].toInt() and 0xFF shl 8).toLong()
        }
        return sum
    }


}