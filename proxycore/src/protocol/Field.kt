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


import kotlin.math.ceil


/**
 * @param name 字段名称
 * @param bits 长度 占多少个bit
 * @param desc 说明
 * @param dynamicBits 非固定长度，即动态长度的计算函数
 * */
class Field(
    val name: String,
    val bits: Int,// unit： bit
    val desc: String? = null,
    val dynamicBits: (() -> Int)? = null
){
    /**
     * 该字段在ByteArray中的起始位置，0开始起算
     * 初始化时根据所占bit自动计算
     * */
    var byteOffset: Int = -1
    /**
     * 在1byte内的偏移位
     * */
    var bitOffset: Int = -1

    /**
     * bit换算成多少个字节数，向上取整
     * */
    val bytes
        get() = if(bits > 0) ceil(bits / 8.0).toInt() else dynamicBits?.let { it() }?:0
}