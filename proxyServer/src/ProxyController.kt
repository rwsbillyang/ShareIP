/*
 * Copyright © 2022 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2022-10-12 17:53
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

package com.github.rwsbillyang.proxy.proxyServer

import com.github.rwsbillyang.ktorKit.apiBox.DataBox
import com.github.rwsbillyang.proxy.Proxy

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


class ProxyController : KoinComponent {
    private val service: ProxyService by inject()

    fun join(proxy: Proxy, natIP: String): DataBox<List<Proxy>>{
        val list = service.findAllProxy()

        proxy.apply {
            time = System.currentTimeMillis()
            ip = natIP
            //中国|0|山东省|济南市|联通
            service.getIpCity(natIP)?.split('|')?.let {
                if(it.isNotEmpty() && it[0] != "0") country = it[0]
                if(it.size > 2 && it[0] != "0") province = it[2]
                if(it.size > 3 && it[0] != "0") city = it[3]
                if(it.size > 4 && it[0] != "0") isp = it[4]
            }
        }
        service.upsertProxy(proxy)
        return DataBox.ok(list)
    }

    fun ip2Address(ip: String) = service.getIpCity(ip)
}
