/*
 * Copyright © 2023 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2023-02-15 11:03
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

package com.github.rwsbillyang.proxy.protocol

/**
 * 设置byte的bit位，返回一个新Byte
 * @param pos bit位, 从右开始为0，最左为7
 * @param bit 写入的bit值： 0 or 1
 * */
fun Byte.setBit(pos: Int, bit: Int): Byte{
    if(pos < 0 ||  pos > 7){
        throw InvalidParameterException(pos)
    }
    if(bit < 0  || bit > 1){
        throw InvalidParameterException(bit)
    }

    val mask = 1 shl pos
    val newInt = if(bit == 0){
        this.toInt() and mask.inv()
    }else{
        this.toInt() or mask
    }
    return newInt.toByte()
}

/**
 * 写入1位，从最高地址的最右开始写起，到最第一个字节的最左侧
 * @param pos 写入位置，范围：[0, ByteArray*8 -1]， 从ByteArray最后一个byte的最右开始计算，初始为0，到第1个字节的最左位置，累计比特位
 * @param bit 待写入的值，0或者1
 * */
fun ByteArray.writeBit(pos: Int, bit: Int){
    val byteNum = pos / 8 //跳过的字节数
    val bitSkip = pos % 8 //某字节上跳过的bit位
    val index = this.size -1 - byteNum
    this[index] = this[index].setBit(bitSkip, bit)
}

/**
 * 读取某1位, 返回1或者0
 * @param byteOffset byte的索引，值范围[0, ByteArray.size]
 * @param bitOffset byte内的bit索引，值范围[0,7], 最左为0，最右为7
 * */
fun ByteArray.readBit(byteOffset: Int, bitOffset: Int)
    = ((this[byteOffset].toInt() and 0xFF) shr (Byte.SIZE_BITS - bitOffset - 1)) and 1

fun ByteArray.toInt():Int?{
    if(size > Int.SIZE_BYTES){
        throw NotMatchBitLengthException("ByteArray.size(${size}) > ${Int.SIZE_BYTES}")
    }
    return when(size){
        4 -> ((this[0].toInt() and 0xFF) shl 24 //第1个byte是最高8位
                or ((this[1].toInt() and 0xFF) shl 16)//第2个byte是次高8位
                or ((this[2].toInt() and 0xFF) shl 8) or ((this[3].toInt()) and 0xFF))
        3 -> ( ((this[0].toInt() and 0xFF) shl 16)//第2个byte是次高8位
                or ((this[1].toInt() and 0xFF) shl 8) or ((this[2].toInt()) and 0xFF))
        2 -> (((this[0].toInt() and 0xFF) shl 8) or ((this[1].toInt()) and 0xFF))
        1 -> (this[0].toInt()) and 0xFF
        else -> null
    }
}

/**
 * 转换成xx.xx.xx.xx形式IP
 * */
fun ByteArray.toIp() = String.format(
    "%s.%s.%s.%s",
    this[0].toInt() and 0x00FF,
    this[1].toInt() and 0x00FF,
    this[2].toInt() and 0x00FF,
    this[3].toInt() and 0x00FF
)
//https://www.baeldung.com/kotlin/byte-arrays-to-hex-strings
fun ByteArray.toHexString() = joinToString(" ") {
    "0x%02x".format(it)  //it.toUInt().toString(radix = 16).padStart(2, '0')
}


/**
 * 整数IP转换成xx.xx.xx.xx形式IP
 * */
fun Int.toIp() = String.format(
    "%s.%s.%s.%s", this shr 24 and 0x00FF,
    this shr 16 and 0x00FF, this shr 8 and 0x00FF, this and 0x00FF
)

/**
 * 用于格式化输出
 * */
val Int.bytes
    get() = when(this){
        0 -> "0"
        1-> "${this}byte"
        else -> "${this}bytes"
    }

/**
 * 用于格式化输出
 * */
val Int.bits
    get() = when{
        this <= 0 -> "${this}"
        this ==1 -> "${this}bit"
        else -> "${this}bits"
    }
/**
 * 用于格式化输出
 * */
fun pos(byteOffset: Int, bitOffset: Int) = "[$byteOffset,$bitOffset]"