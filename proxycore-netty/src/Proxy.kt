/*
 * Copyright © 2022 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2022-10-12 15:11
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

import kotlinx.serialization.Serializable



@Serializable
data class Proxy(
    val _id: String, //devId
    var ipv6: String?,
    var ipv4: String?,
    var ip: String?, //NAT public IP
    var port: Int,
    var status: Int = 1, //0表示暂不可用，-1表示待剔除
    var time: Long = System.currentTimeMillis(),//最新更新时间
    var country: String? = null, //center server set it
    var province: String? = null,
    var city: String? = null,
    var isp: String? = null
)



