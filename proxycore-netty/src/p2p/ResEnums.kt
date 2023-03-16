/*
 * Copyright © 2022 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2022-10-11 13:23
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

package com.github.rwsbillyang.proxy.p2p


enum class Keys{ category, data, code, msg}

/**
 * value of key "category"
 * */
enum class RequestCategory{ Join, Config}

/**
 * value of key "category"
 * */
enum class ResponseCategory{ Proxies, Config }

/**
 * value of key "code"
 * */
enum class Code{OK, KO} //same as Code in ktorkit library

//request format: category=Join&data={payload json}
//response format: code=OK&category=Proxies&data={payload json} or code=OK&category=Proxies&msg=errorMsg

