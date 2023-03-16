/*
 * Copyright © 2023 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2023-02-11 12:20
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

package com.github.rwsbillyang.proxy.tunnel

import com.github.rwsbillyang.proxy.HeaderUtil
import com.github.rwsbillyang.proxy.protocol.toIp


/**
 * @param dest destination IP 若为0表示心跳消息
 * @param port destination port
 * @param data application data
 * */
class TunnelData(val dest: Int, val port: Short?, val data: ByteArray?){
    override fun toString()= "destIp=${dest.toIp()}, port=${port}, data.size=${data?.size}"
}

