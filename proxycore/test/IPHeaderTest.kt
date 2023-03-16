


/*
 * Copyright © 2023 rwsbillyang@qq.com 
 *  
 * Written by rwsbillyang@qq.com at Beijing Time: 2023-02-15 20:07 
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

package com.github.rwsbillyang.proxy.test

import com.github.rwsbillyang.proxy.protocol.tcpip.IPHeader
import com.github.rwsbillyang.proxy.protocol.toIp
import org.junit.Test
import kotlin.test.assertEquals


fun byteArrayOfInts(vararg ints: Int) = ByteArray(ints.size) { pos -> ints[pos].toByte() }

class IPHeaderTest {
    private val SYN_ACK_PACKET = byteArrayOfInts(
        0x00, 0x10, 0x7b, 0x38,//Version(4bits) + HeaderLength(4bits)+TypeOfService(8bits) + totalLength(16bits)
        0x46, 0x33, 0x08, 0x00,// ID(16bits) + Flag
        0x20, 0x89, 0xa5, 0x9f,//TTL(8bits) + Protocol(8bits) + Checksums(16bits)
        0x08, 0x00, 0x45, 0x00,//SrcIP
        0x00, 0x2c, 0x93, 0x83,//DestIP
        0x40, 0x00, 0xff, 0x06,
        0x6c, 0x38, 0xac, 0x10,
        0x70, 0x32, 0x87, 0x0d,
        0xd8, 0xbf, 0x00, 0x19,
        0x50, 0x49, 0x78, 0xbe,
        0xe0, 0xa7, 0x9f, 0x3a,
        0xb4, 0x03, 0x60, 0x12,
        0x22, 0x38, 0xfc, 0xc7,
        0x00, 0x00, 0x02, 0x04,
        0x05, 0xb4, 0x70, 0x6c
    )
    private val PSH_ACK_PACKET = byteArrayOfInts(
        0x08, 0x00, 0x20, 0x89,
        0xa5, 0x9f, 0x00, 0x10,
        0x7b, 0x38, 0x46, 0x33,
        0x08, 0x00, 0x45, 0x00,
        0x00, 0x3e, 0x87, 0x08,
        0x40, 0x00, 0x3f, 0x06,
        0x38, 0xa2, 0x87, 0x0d,
        0xd8, 0xbf, 0xac, 0x10,
        0x70, 0x32, 0x50, 0x49,
        0x00, 0x19, 0x9f, 0x3a,
        0xb4, 0x03, 0x78, 0xbe,
        0xe0, 0xf8, 0x50, 0x18,
        0x7d, 0x78, 0x86, 0xf0,
        0x00, 0x00, 0x45, 0x48,
        0x4c, 0x4f, 0x20, 0x61,
        0x6c, 0x70, 0x68, 0x61,
        0x2e, 0x61, 0x70, 0x70,
        0x6c, 0x65, 0x2e, 0x65,
        0x64, 0x75, 0x0d, 0x0a
    )
    //@Test
    fun test1(){
        val ipHeader = IPHeader(SYN_ACK_PACKET).apply { init() }
        println(ipHeader.toString())
        assertEquals("172.16.112.50", ipHeader.getBits(IPHeader.SrcIP)?.toIp())
        assertEquals("135.13.216.191", ipHeader.getBits(IPHeader.DestIP)?.toIp())
        assert(false){"show test resutl"}
    }
    @Test
    fun test2(){
        val ipHeader = IPHeader(PSH_ACK_PACKET).apply { init() }
        println(ipHeader.toString())
        //assertEquals("172.16.112.50", ipHeader.getBits(IPHeader.SrcIP)?.toIp())
        //assertEquals("135.13.216.191", ipHeader.getBits(IPHeader.DestIP)?.toIp())
        assert(false){"show test resutl"}
    }
}