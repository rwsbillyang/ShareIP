/*
 * Copyright © 2022 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2022-10-12 17:51
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

import com.github.rwsbillyang.ktorKit.cache.ICache
import com.github.rwsbillyang.ktorKit.db.MongoDataSource
import com.github.rwsbillyang.ktorKit.db.MongoGenericService
import com.github.rwsbillyang.proxy.Proxy
import kotlinx.coroutines.runBlocking
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import org.lionsoul.ip2region.xdb.Searcher
import org.litote.kmongo.coroutine.CoroutineCollection
import org.slf4j.LoggerFactory
import java.io.IOException

class ProxyService(cache: ICache) : MongoGenericService(cache) {
    private val log = LoggerFactory.getLogger("ProxyService")
    private val dbSource: MongoDataSource by inject(qualifier = named(proxyAppModule.dbName!!))

    private val proxyCol: CoroutineCollection<Proxy> by lazy {
        dbSource.mongoDb.getCollection()
    }

    fun findAllProxy() = runBlocking{
        proxyCol.find().toList()
    }

    fun upsertProxy(proxy: Proxy) = save(proxyCol, proxy)



    //https://gitee.com/lionsoul/ip2region/tree/master/binding/java#%E7%BC%93%E5%AD%98-vectorindex-%E7%B4%A2%E5%BC%95
    fun getIpCity(ip: String, useRawData:Boolean = false): String? {
        // 备注：每个线程需要单独创建一个独立的 Searcher 对象，但是都共享全局的制度 vIndex 缓存。
        val searcher = try {
            Searcher.newWithFileOnly("ip2region.xdb")
        } catch (e: IOException) {
            log.warn("failed to create searcher with ip2region.xdb: ${e.message}")
            return null
        }
        //cacheIncludeNull
        val value = cache[ip]
        if(value == null){
            return try {
                if(useRawData) searcher.search(ip)
                else searcher.search(ip) // 中国|0|山东省|济南市|联通
                    .split('|')
                    .filterIndexed { i, v ->
                        when (i) {
                            0 -> v != "中国" && v != "0"  //若是 中国 则省略
                            1 -> false // 去掉区域
                            2, 3 -> v != "0" // 省市不为0则保留
                            4 -> true
                            else -> false
                        }
                    }.joinToString(" ") // 查询
            }catch (e: Exception) {
                log.warn("failed to search($ip): ${e.message}")
                cache.put(ip, "")
                null
            }
        }else{
           return if(value == "")
               null
            else value.toString()
        }


    }
}
