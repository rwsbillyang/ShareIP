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
 * 未找到字段异常
 * 要么拼写错误，要么未配置
 * */
class FieldNotFoundException(key: String): Exception("FieldNotFoundException: not found field: $key, please check field config")

/**
 * 所占bit不是完整字节，即不能被8整除，需配置bit mask
 * */
class NoBitMaskException(f: Field): Exception("NoBitMaskException:no bitmask, field=${f.name}, bits=${f.bits}, cannot divided by 8, please check field config")

/**
 * 动态字节数，bits配置为-1，需指定dynamicBits
 * */
class NoDynamicBitsFunctionException(f: Field): Exception("NoBitMaskException:no bitmask, field=${f.name}, bits=${f.bits} < 0, should config dynamicBits, please check field config")
/**
 * 如对多bit字段调用getBool，对多于4字节的非Int字段获取Int值
 * */
class NotMatchBitLengthException(msg: String): Exception(msg)

/**
 * 调用参数异常，比如超出7的bit索引等
 * */
class InvalidParameterException(v: Int): Exception("InvalidParameterException: $v")