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
data class Config(
    /**
     * socks5是否认证的开关
     */
    var isAuth: Boolean = true,

    /**
     * socks5服务绑定的端口号
     */
    var socks5Port: Int = 27000,
    var socks5Pwd: String = "IWantShareIp",

    /**
     * socks5认证文件路径
     */
    var authPath: String? = null,

    /**
     * socks5黑名单路径
     */
    var blacklist: String? = null,

    //root server: request proxy list info
    var server: String = "122.114.79.157",
    var port: Int = 26000
)
