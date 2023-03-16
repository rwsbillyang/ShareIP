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


import org.slf4j.LoggerFactory
import kotlin.math.ceil
import kotlin.math.floor



/**
 * 继承此抽象类的协议，只需列出各字段Field的信息，将自动解析出各字段的值
 * @param data 协议数据所在的缓冲区字节数组 协议数据为Big Endian
 * @param offset 从offset处开始解析
 *
 * Big Endian 是指低址存放高位字节。假设有16进制数：0x12FA，其中12为高位字节，FA为地位字节，
 * 那么data中存放顺位为：1 2 F A,  即第1个byte存放12（低4位存放1，高4位存放2），第2个byte存放的是FA（低4位存放F，高4位存放A）
 * 也就是0x12FA字面量顺序与地址顺序一致
 *
 * Little Endian 是指低地址端存放低位字节。数据类型转换非常方便.
 * 假设有16进制数：12FA，则存放顺序为: A F 2 1 其中A的地址低，1的地址最高
 * */
abstract class ProtocolHeader(val data: ByteArray, val offset: Int = 0) {
    val log = LoggerFactory.getLogger("ProtocolHeader")

    private val fieldsMap = mutableMapOf<String, Field>()

    /**
     * 1.需按协议顺序列出
     * 2.bits小于0时，表示长度动态，需提供计算方法dynamicBits
     * 基类构造函数执行时，派生类中声明或覆盖的属性都还没有初始化
     * 计一个基类时，应该避免在构造函数、属性初始化器以及 init 块中使用 open 成员。
     * */
    protected abstract val fields: List<Field>

    /**
     * 返回协议头长度，单位字节数
     * */
    //abstract fun getHeaderLength(): Int
    /**
     * 返回payload data长度，单位字节数
     * */
    //abstract fun getDataLength(): Int

    fun init(){
        var totalBits = 0
        log.info("init: calculate byteOffset, bitOffset, dynamicBits of every field")
        fields.forEach {
            it.byteOffset = offset + floor(totalBits / 8.0).toInt()
            it.bitOffset = totalBits - it.byteOffset * 8 //totalBits % 8
            totalBits += (if(it.bits > 0) it.bits else it.dynamicBits?.let { it() }?:0)

            fieldsMap[it.name] = it
        }
    }

    override fun toString() = fields.joinToString("\n") {
        val bytes =  getBits(it)
        if(bytes == null){
            "No field: ${it.name}, ${it.bits.bits}, offset:${pos(it.byteOffset,it.bitOffset)}, ${it.desc}"
        }else{
            val hex = bytes.toHexString()
            if(bytes.size > Int.SIZE_BYTES){
                "${it.name}(${it.bits.bits}), offset:${pos(it.byteOffset,it.bitOffset)}, ($hex), ${it.desc}"
            }else{
                "${it.name}(${it.bits.bits}), offset:${pos(it.byteOffset,it.bitOffset)}, ${bytes.toInt()}($hex), ${it.desc}"
            }
        }
    }

    /**
     * 获取某个字段的配置信息，可用于协议解析说明
     * */
    fun getField(key: String) = fieldsMap[key]

    /**
     * 是否设置了标志，即值1。只有1位的字段才可调用getBool
     * */
    fun getBool(key: String): Boolean{
        val field = fieldsMap[key]
        if(field == null){
            log.warn("getBool: no field, name=$key, please check spelling")
            throw FieldNotFoundException(key)
        }
        if(field.bits != 1){
            log.warn("getBool: name=$key, ${field.bits.bits} > 1, please check config or wrongly call?")
            throw NotMatchBitLengthException("getBool only need 1bit, but got ${field.bits.bits} in field: $key")
        }

        return data.readBit(field.byteOffset, field.bitOffset) == 1
    }
    /**
     * 获取某个字段的值，已做过右对齐转换操作
     * */
    fun getInt(key: String) = getBits(key)?.toInt()
    /**
     * 获取某个字段的值，已做过右对齐转换操作
     * */
    fun getInt(field: Field) = getBits(field)?.toInt()

    /**
     * 在data字节数组中，从data缓冲中度取多个bit，支持跨字节的多bit读取，读取结果直接用于转换成Int
     * 高地址低位字节向右对齐，低地址高位字节掩码消除非自己的bits
     * 返回的bytes数组与Int内部bit布局一致，无需再左移右移等运算
     * */
    fun getBits(key: String): ByteArray?{
        val field = fieldsMap[key]
        if(field == null){
            log.warn("getBits: no field, name=$key, please check spelling")
            throw FieldNotFoundException(key)
        }

        return getBits(field)
    }
    /**
     * 在data字节数组中，从data缓冲中度取多个bit，支持跨字节的多bit读取，读取结果直接用于转换成Int
     * 高地址低位字节向右对齐，低地址高位字节掩码消除非自己的bits
     * 返回的bytes数组与Int内部bit布局一致，无需再左移右移等运算
     * */
    fun getBits(field: Field) : ByteArray?{
        if(field.bits < 0 && field.dynamicBits == null){
            log.warn("no dynamicBits")
            throw NoDynamicBitsFunctionException(field)
        }
        val fieldBitsNum = if(field.bits < 0) field.dynamicBits?.let { it() }?:0 else field.bits //total bits
        if(fieldBitsNum <= 0) return null


        val offset = field.byteOffset
        val bitOffset = field.bitOffset
        val endBitOffset = (bitOffset + fieldBitsNum) % 8 //bits长度位所占最后1位的位偏移

        val bytes = ceil(fieldBitsNum / 8.0).toInt() //存储所需字节数

        log.info(" ")
        log.info("${field.name}(${field.bits.bits}) need ${bytes.bytes} storage")

        val buf = ByteArray(bytes)

        if(endBitOffset > 0){//高地址低字节，右移对齐
            for(count in (0 until fieldBitsNum)){
                val unreadCountTotal = fieldBitsNum - count //剩余未读bit数
                val countInFirstByte =  8 - bitOffset //最左侧的字节内的bit数
                val curOffset = if(unreadCountTotal <= countInFirstByte) offset else offset + ceil((unreadCountTotal - countInFirstByte) / 8.0).toInt()
                val curBit = if(unreadCountTotal <= countInFirstByte) bitOffset + unreadCountTotal-1 else (unreadCountTotal - countInFirstByte - 1) % 8 //减1是从数量变成0开始的索引
                val bit = data.readBit(curOffset , curBit)//从最右侧一个bit读取，读到最左侧

                log.info("${field.name} read 1bit from ${pos(curOffset,curBit)}")

                buf.writeBit(count, bit)
            }
        }else{// endBitOffset == 0 高地址低位字节本来就右对齐
            data.copyInto(buf,0, offset,  offset+bytes)

            log.info("${field.name} copy ${bytes.bytes} from offset:${pos(offset,bitOffset)}")

            if(bitOffset > 0){//清除非自己的高位bit
                val mask = (1 shl (8-bitOffset)) - 1 //bitOffset起之后的1byte之内的位全是1
                buf[0] = (buf[0].toInt() and mask).toByte()

                log.info("${field.name} clear left ${(8-bitOffset).bits} done")
            }

            //案例分析:
            //1. 共4bits已向右对齐，则其bitOffset为4，得mask为0x0F，则buf[0]高4位被清空
            //2. 共13bits已向右对齐，则其还余5bits在前1字节上，bitOffset为3, mask为0b0001_1111, 2字节被复制后，第1字节高3位被清空
        }

        return buf
    }

    /**
     * alias to create Filed
     * */
    protected fun f(name: String, bits: Int, desc: String? = null, dynamicBits: (() -> Int)? = null): Field {
        val field = Field(name, bits, desc, dynamicBits)
       if(bits < 0 && dynamicBits == null){
           log.error("no dynamicBits provided when bits < 0")
           throw NoDynamicBitsFunctionException(field)
       }
        return field
    }


    /**
     * 在data字节数组中，从offset起始的2字节，转换为Short类型
     * */
    fun readShort(offset: Int): Short {
        val r = ((data[offset].toInt() and 0xFF) shl 8) or (data[offset + 1].toInt() and 0xFF)
        return r.toShort()
    }

    /**
     * 在data字节数组中，从offset起始的4字节，转换为Int
     * */
    fun readInt(offset: Int): Int {
        return ((data[offset].toInt() and 0xFF) shl 24 //第1个byte是最高8位
                or ((data[offset + 1].toInt() and 0xFF) shl 16)//第2个byte是次高8位
                or ((data[offset + 2].toInt() and 0xFF) shl 8) or ((data[offset + 3].toInt()) and 0xFF))
    }

    fun readInt(buf:ByteArray): Int {
        return ((buf[0].toInt() and 0xFF) shl 24 //第1个byte是最高8位
                or ((buf[1].toInt() and 0xFF) shl 16)//第2个byte是次高8位
                or ((buf[2].toInt() and 0xFF) shl 8) or ((data[3].toInt()) and 0xFF))
    }


    /**
     * 根据该字段所占bit数量，返回不同类型的值
     * (32,) -> ByteArray
     * (16,32] -> Int
     * (8, 16] -> Short
     * (0, 8] -> Byte
     * 0 -> null
     * <0 -> ByteArray
     * */
//    fun getValue(key: String): Any{
//        val field = fieldsMap[key]
//        if(field == null){
//            log.warn("no field, name=$key, please check spelling")
//            throw FieldNotFoundException(key)
//        }
//
//        val len = field.bits
//        return when{
//            len > Int.SIZE_BITS -> readBytes(field)
//            len == Int.SIZE_BITS -> readInt(field.byteOffset)
//            len < Int.SIZE_BITS && len > Short.SIZE_BITS -> readInt(field.byteOffset)
//            len ==  Short.SIZE_BITS -> readShort(field.byteOffset)
//            len < Short.SIZE_BITS && len > Byte.SIZE_BITS -> readShort(field.byteOffset)
//            len == Byte.SIZE_BITS -> data[field.byteOffset]
//            len < Byte.SIZE_BITS && len > 1 -> data[field.byteOffset]
//            len == 1 -> (data[field.byteOffset].toInt() and 0xFF) == field.bitMask
//            else -> {
//                log.warn("invalid offset in Field:${field.byteOffset}, no custom decode function. ignore it")
//                throw NoDynamicBitsFunctionException(field)
//            }
//        }
//    }

    /**
     * 适用于bit长度的字段的值：[1,32]
     * 对于非完整字节，将去掉其填充字段，并做了移位转换成Int值
     * 不支持跨字节读取
     * */
//    fun getInt(key: String): Int?{
//        val field = fieldsMap[key]
//        if(field == null){
//            log.warn("getInt: no field, name=$key, please check spelling")
//            throw FieldNotFoundException(key)
//        }
//        val len = if(field.bits > 0) field.bits else field.dynamicBits?.let { it() }?:throw NoDynamicBitsFunctionException(field)
//
//        return when{
//            len == Int.SIZE_BITS -> readInt(field.byteOffset)
//            len < Int.SIZE_BITS && len > Short.SIZE_BITS -> {
//                if(field.bitMask == null)
//                    throw NoBitMaskException(field)
//                (readInt(field.byteOffset) and field.bitMask) shr (Int.SIZE_BITS-field.bitOffset - field.bits)
//            }
//            len ==  Short.SIZE_BITS -> readShort(field.byteOffset).toInt() and 0xFFFF
//            len < Short.SIZE_BITS && len > Byte.SIZE_BITS -> {
//                if(field.bitMask == null)
//                    throw NoBitMaskException(field)
//                //横跨2个字节
//                val shift = Short.SIZE_BITS-field.bitOffset - field.bits
//                if(shift >= 0)
//                    (readShort(field.byteOffset).toInt() and field.bitMask ) shr shift
//                else{//横跨3个字节,比如13bits，分别在3个字节里面：2+8+3
//                    1
//                }
//            }
//            len == Byte.SIZE_BITS -> data[field.byteOffset].toInt() and 0xFF
//            len < Byte.SIZE_BITS && len > 1 ->{
//                if(field.bitMask == null)
//                    throw NoBitMaskException(field)
//
//                val overflow = field.bitOffset + field.bits - Byte.SIZE_BITS
//                if(overflow >= 0){//横跨2个字节，比如6字节，分别在2个字节里面：3+3
//                    val left =((data[field.byteOffset].toInt() and (field.bitMask shr overflow))) shl overflow
//                    val right = (data[field.byteOffset+1].toInt() and 0xFF) shr overflow
//                    left or right
//                }else{
//                    (data[field.byteOffset].toInt() and (1 shl field.bits - 1)) shr -overflow
//                }
//            }
//            len == 1 -> (data[field.byteOffset].toInt() and 0xFF) shr (Byte.SIZE_BITS-field.bitOffset - field.bits)
//            len == 0 -> null
//            else ->{
//                log.warn("getInt: name=$key, bits(${len}) < 2 or >32, please check config or wrongly call?")
//                throw NotMatchCallException(field)
//            }
//
//        }
//    }

}

