/*
 * Copyright © 2023 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2023-02-12 16:44
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

package com.github.rwsbillyang.proxy.socket

import java.io.IOException
import java.net.InetSocketAddress
import java.nio.channels.AsynchronousChannelGroup
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors


class TcpServer {
    fun start(port: Int, isAsync:Boolean){

        val serverChannel = try {
            AsynchronousServerSocketChannel
                .open(AsynchronousChannelGroup.withThreadPool(Executors.newFixedThreadPool(2)))
                .bind(InetSocketAddress(port))
        } catch (ioe: IOException) {
            ioe.printStackTrace()
            AsynchronousServerSocketChannel.open().bind(InetSocketAddress(port))
        }

        if(isAsync){
            serverChannel.accept(null, object: CompletionHandler<AsynchronousSocketChannel, Any?>{
                override fun completed(result: AsynchronousSocketChannel, attachment: Any?) {
                    // 接收到新的客户端连接时调用，result就是和客户端的连接对话，此时可以通过result和客户端进行通信
                    System.out.println("accept completed");

                  //  asyncHandle(result);  // 异步处理连接
                    // 继续监听accept
                    serverChannel.accept(null, this);
                }

                override fun failed(exc: Throwable?, attachment: Any?) {
                    TODO("Not yet implemented")
                }
            })
        }else{
            try {
                while (true) {
                    val conn = serverChannel.accept()
                    val asyncSocketChannel = conn.get() // 阻塞等待直到future有结果
                  //  asyncHandle(asyncSocketChannel)// 异步处理连接
                }
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            } catch (e: ExecutionException) {
                e.printStackTrace()
            }
        }

    }


}