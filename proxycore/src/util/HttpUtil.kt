/*
 * Copyright © 2022 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2022-10-14 20:21
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

package com.github.rwsbillyang.proxy.util


import java.io.ByteArrayOutputStream
import java.io.IOException

import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.SocketTimeoutException
import java.net.URL

object HttpUtil {
    //private val log = LoggerFactory.getLogger("HttpUtil")

    /**
     * 获取 POST请求
     * @return 请求结果
     */
    fun doPost(
        url: String,
        params: String?,
        userAgent: String? = "Android ShareIP",
        connectTimeout: Int = 30 * 1000,
        readTimeout: Int = 30 * 1000,
        contentType: String = "application/json"
    ): String? {
        val heads = mapOf(Pair("user-agent", userAgent ?: "Android ShareIP"))
        return doRequest("POST", url, params, connectTimeout, readTimeout, contentType, heads)
    }


    /**
     * 获取 POST请求
     *
     * @return 请求结果
     */
    fun doGet(
        url: String,
        params: Map<String, String>?,
        userAgent: String? = "Android ShareIP",
        connectTimeout: Int = 30 * 1000,
        readTimeout: Int = 30 * 1000,
        contentType: String = "application/json"
    ): String? {
        // 拼接请求参数
        var urlWithQuery = url
        val q = if (!params.isNullOrEmpty()) {
            params.entries.joinToString("&") { "${it.key}=${it.value}" }
        } else null
        if (q != null)
            urlWithQuery += "?$q"
        val heads = mapOf(Pair("user-agent", userAgent ?: "Android ShareIP"))
        return doRequest("GET", urlWithQuery, null, connectTimeout, readTimeout, contentType, heads)
    }

    fun doRequest(
        method: String,
        url: String,
        postData: String?,
        connectTimeout: Int,
        readTimeout: Int,
        contentType: String,
        heads: Map<String, String>?
    ): String? {
        try {
            val u = URL(url)
            val connection = u.openConnection() as HttpURLConnection
            connection.requestMethod = method
            connection.useCaches = false
            connection.instanceFollowRedirects = true
            connection.connectTimeout = connectTimeout
            connection.readTimeout = readTimeout
            connection.setRequestProperty("Content-Type", contentType)
            heads?.entries?.forEach {
                connection.setRequestProperty(it.key, it.value)
            }
            connection.doInput = true

            if (method.lowercase() == "post" && !postData.isNullOrEmpty()) {
                connection.doOutput = true
                val out = connection.outputStream
                out.write(postData.toByteArray())
                out.flush()
                out.close()
            }

            // 连接
            connection.connect()
            // 得到响应状态码的返回值 responseCode
            val code = connection.responseCode
            // 5. 如果返回值正常，数据在网络中是以流的形式得到服务端返回的数据
            var msg: String? = null
            if (code == 200) { // 正常响应
                // 从流中读取响应信息
                val outStream = ByteArrayOutputStream()
                val buffer = ByteArray(1024)
                var len = 0
                while (connection.inputStream.read(buffer).also { len = it } != -1) {
                    outStream.write(buffer, 0, len)
                }
                connection.inputStream.close()
                msg = outStream.toString()
                outStream.close()
            }else{
                System.err.println("HttpUtil: response code: $code, url=$url")
            }
            // 6. 断开连接，释放资源
            connection.disconnect()
            return msg
        } catch (e: MalformedURLException) {
            System.err.println("HttpUtil: MalformedURLException: ${e.message} ")
        } catch (e: SocketTimeoutException) {
            System.err.println("HttpUtil: SocketTimeoutException: ${e.message} ")
            e.printStackTrace()
        } catch (e: IOException) {
            System.err.println("HttpUtil: IOException: ${e.message} ")
            e.printStackTrace()
        } catch (e: IllegalStateException) {
            System.err.println("HttpUtil: IllegalStateException: ${e.message} ")
            e.printStackTrace()
        } catch (e: Exception) {
            System.err.println("HttpUtil: Exception: ${e.message} ")
            e.printStackTrace()
        }
        return null
    }

}

