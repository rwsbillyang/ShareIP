/*
 * Copyright © 2022 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2022-10-14 20:15
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


import com.github.rwsbillyang.proxy.p2p.*
import com.github.rwsbillyang.proxy.util.HttpUtil
import com.github.rwsbillyang.proxy.util.IPv6Util

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json



@Serializable
class DataBoxProxyList(
    val code: String,
    val msg: String? = null,
    val data: List<Proxy>? = null
)

/**
 * @param devId deviceId, if no, need generate by app
 * @param proxyList if provided, load proxy list from cache
 * @param config
 * */
class ProxyManager(private val devId: String, proxyList: List<Proxy>? = null, var config: Config?) {
    //private val log = LoggerFactory.getLogger("ProxyManager")

    private val proxyMap = mutableMapOf<String, Proxy>()

    init {
        proxyList?.forEach {
            proxyMap[it._id] = it
        }
        if(config == null) config = Config()
    }


    /**
     * 得到一个最新的join peer后的callback，android和Linux Server可以有不同的反应，如查询所属省市
     * */
    private var _onGetJoinCallback: ((proxy: Proxy, remoteIp: String) -> Unit)? = null

    /**
     * 得到更新后的proxy list后的callback，android和Linux Server可以将其序列化，查询ip地址的address等
     * */
    private var _onGetProxiesCallback: ((list: List<Proxy>) -> Unit)? = null

    //序列化，供下次使用最新值
    private var _onServerConfigUpdate: ((config: Config) -> Unit)? = null

    fun onGetProxyList(cb: (list: List<Proxy>) -> Unit){
        _onGetProxiesCallback = cb
    }
    fun onServerConfigUpdate(cb: (config: Config) -> Unit){
        _onServerConfigUpdate = cb
    }
    fun onGetJoin(cb: (proxy: Proxy, remoteIp: String) -> Unit){
        _onGetJoinCallback = cb
    }
    /**
     * join proxy network
     * get proxy list
     * return true if successful
     * */
    fun joinByTcp(server: String = config!!.server, port: Int = config!!.port): Boolean{
        val proxy = getSelfProxy(devId)
        val msgToSend = makeRequest(RequestCategory.Join, Json.encodeToString(proxy))
        return if(proxy != null){
            PeerAsClient(this, msgToSend).connectToServer(server, port)
        }else{
            println("no ipv6 address?")
             false
        }
    }

    fun joinByHttp(url: String): Boolean{
        val proxy = getSelfProxy(devId)
        return if(proxy != null){
            HttpUtil.doPost(url, Json.encodeToString(proxy))?.let {
                val box = Json.decodeFromString<DataBoxProxyList>(it)
                if(box.code == Code.OK.name){
                    if(box.data != null)
                        handleClientGetProxyList(box.data)
                    else{
                        println("no data in box")
                    }
                    true
                }else{
                    false
                }
            }?:false
        }else{
            println("no ipv6 address?")
            false
        }
    }




//    fun broadcastToTopPeers(topNumber: Int){
//        val list = proxyMap.toList().map { it.second }.sortedByDescending { it.time }
//    }

    private fun getSelfProxy(devId: String): Proxy?{
        val ipv4 = IPv6Util.getLocalIPAddress(false)
        val ipv6 = IPv6Util.getLocalIPAddress() ?: return null
        return Proxy(devId, ipv6, ipv4, null, config!!.socks5Port)
    }

    //client make a request
    private fun makeRequest(category: RequestCategory, data: String): String{
        val map = mutableMapOf<String, String>()
        map[Keys.category.name] = category.name
        map[Keys.data.name] = data
        return map.entries.joinToString("&") {
            "${it.key}=${it.value}"
        }
    }

    //server handle request from client
    fun handleRequest(msg: String, remoteIp: String): String?{
        if(!msg.contains(Keys.category.name)){
            println("not support request msg: $msg")
            return null
        }

        val map = parseMsg(msg)
        when(val category = map[Keys.category.name]){
            RequestCategory.Join.name -> {
                val json = map[Keys.data.name]
                if(json != null){
                    handleServerGetProxy(Json.decodeFromString(json), remoteIp)
                }else{
                    println("no payload data, msg=$msg")
                }
            }
            else -> {
                println("not support category=$category, msg=$msg")
            }
        }
        return null
    }

    //client handle response from server
    fun handleResponse(msg: String): String{
        val code = Keys.code.name
        if(!msg.contains(Keys.category.name) || !msg.contains(code)){
            println("not support msg: $msg")
            return Code.KO.name
        }

        msg.split('\n').forEach {
            val map = parseMsg(it)
            if(map[code] != Code.OK.name){
                println("something wrong: ${map[Keys.msg.name]}, msg=$msg")
            }else{
                val json = map[Keys.data.name]
                if(json == null){
                    println("no payload data, msg=$msg")
                }else{
                    when(val category = map[Keys.category.name]){
                        ResponseCategory.Proxies.name -> handleClientGetProxyList(Json.decodeFromString(json))
                        ResponseCategory.Config.name -> handleServerConfigUpdate(Json.decodeFromString(json))
                        else -> println("not support category=$category, msg=$msg")
                    }
                }
            }
        }

        return Code.OK.name
    }

    private fun parseMsg(msg: String): Map<String, String>{
        val map = mutableMapOf<String, String>()
        msg.split('&').forEach {
            val kv = it.split('=')
            if(kv.size == 2)map[kv[0]] = kv[1]
            else{
                println("invalid key=value: $it, msg=$msg")
            }
        }
        return map
    }

    // client get proxy list
    private fun handleClientGetProxyList(list: List<Proxy>){
        //merge proxy
        list.forEach {
            proxyMap[it._id] = it
        }
        //proxyMap.remove(devId) //remove self

        _onGetProxiesCallback?.let{
            it(proxyMap.entries.map { it.value })
        }
    }

    //server get proxy join
    private fun handleServerGetProxy(proxy: Proxy, remoteIp: String){
        proxyMap[proxy._id] = proxy.apply {
            time = System.currentTimeMillis()
        }
        _onGetJoinCallback?.let {
            it(proxy, remoteIp)
        }

    }

    private fun handleServerConfigUpdate(newConfig: Config){
        config = newConfig
        _onServerConfigUpdate?.let {
            it(newConfig)
        }
    }
}
