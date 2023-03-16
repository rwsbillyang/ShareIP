/*
 * Copyright © 2022 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2022-10-12 17:52
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

import com.github.rwsbillyang.ktorKit.server.AppModule
import com.github.rwsbillyang.ktorKit.server.respondBox
import com.github.rwsbillyang.ktorKit.server.respondBoxOK
import com.github.rwsbillyang.proxy.ClientInfo
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.dsl.module
import org.koin.ktor.ext.inject

val proxyAppModule = AppModule(
    listOf(
        module {
            single { ProxyService(get()) }
            single { ProxyController() }
        }),
    "Proxy"
) {
    proxyApi()
}


fun Routing.proxyApi() {
    val service: ProxyService by inject()
    val controller: ProxyController by inject()

    route("/api/proxy") {
        post("/join") {
            call.respondBox(controller.join(call.receive(), call.request.origin.remoteHost))
        }
        get("/list") { call.respondBoxOK(service.findAllProxy()) }

        get("/ip"){
            val origin = call.request.origin //call.request.origin: {"host":"wx.niukid.com","port":443,"schema":"https","version":"HTTP/1.0","uri":"/api/proxy/ip","remoteHost":"123.232.32.209","ua":"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.0.0 Safari/537.36","city":"中国|0|山东省|济南市|联通"}
            call.respond(
                ClientInfo(origin.host, origin.port, origin.scheme, origin.version, origin.uri,
                    origin.remoteHost, controller.ip2Address(origin.remoteHost), call.request.userAgent())
            )
        }
    }
}

