/*
 * Copyright © 2022 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2022-10-12 11:53
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

import java.io.*
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket


object SocketUtil {
    fun TCPServer(port: Int) {
        try {
            val serverSocket = ServerSocket(port)  //创建服务器端 Socket，指定监听端口

            val socket: Socket = serverSocket.accept()//等待客户端连接
            val inputStream: InputStream = socket.getInputStream()//获取输入流，
            val isr = InputStreamReader(inputStream)
            val bufferReader = BufferedReader(isr)
            var data: String?

            while (bufferReader.readLine().also { data = it } != null) {//读取数据
                println("服务器接收到客户端的数据：$data")
            }

            socket.shutdownInput()//关闭输入流

            val os: OutputStream = socket.getOutputStream()//获取输出流
            val pw = PrintWriter(os)
            //向客户端发送数据
            pw.print("服务器给客户端回应的数据")
            pw.flush()

            socket.shutdownOutput() //关闭输出流

            pw.checkError()
            os.close()
            bufferReader.close()
            isr.close()
            inputStream.close()
            socket.close()//关闭资源
            serverSocket.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun TCPClient() {
        try {
            //创建客户端Socket，指定服务器的IP地址和端口
            val socket = Socket(InetAddress.getLocalHost(), 8888)
            //获取输出流，向服务器发送数据
            val os = socket.getOutputStream()
            val pw = PrintWriter(os)
            pw.write("客户端给服务器端发送的数据")
            pw.flush()
            //关闭输出流
            socket.shutdownOutput()

            //获取输入流，接收服务器发来的数据
            val `is` = socket.getInputStream()
            val isr = InputStreamReader(`is`)
            val br = BufferedReader(isr)
            var data: String?
            //读取客户端数据
            while (br.readLine().also { data = it } != null) {
                println("客户端接收到服务器回应的数据：$data")
            }
            //关闭输入流
            socket.shutdownInput()

            //关闭资源
            br.close()
            isr.close()
            `is`.close()
            pw.close()
            os.close()
            socket.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
