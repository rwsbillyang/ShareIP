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

import java.net.*
import java.util.*


object IPv6Util {
    //private val log = LoggerFactory.getLogger("IPUtil")

    /**
     * 输入 2002:97b:e7aa::97b:e7aa，上述代码执行过后，零压缩部分将被还原，
     * ipAddr变为 2002:97b:e7aa:0:0:0:97b:e7aa
     * */
    fun totalAddress(ipv6: String) = InetAddress.getByName(ipv6).hostAddress


    fun getLocalIPAddress(useIPv6: Boolean = true): String? {
        try {
            var inetAddress: InetAddress? = null
            val networkInterfaces: Enumeration<NetworkInterface> = NetworkInterface.getNetworkInterfaces()
            outer@ while (networkInterfaces.hasMoreElements()) {
                val inetAds: Enumeration<InetAddress> = networkInterfaces.nextElement().getInetAddresses()
                while (inetAds.hasMoreElements()) {
                    inetAddress = inetAds.nextElement()
                    if(useIPv6){
                        //Check if it's ipv6 address and reserved address
                        if (inetAddress is Inet6Address && !isReservedAddr(inetAddress)
                        ) {
                            break@outer
                        }
                    }else{
                        if(inetAddress is Inet4Address && !(inetAddress.isLoopbackAddress || inetAddress.isAnyLocalAddress))
                            return inetAddress.hostAddress
                    }

                }
            }
            var ipAddr = inetAddress?.hostAddress
            if(ipAddr != null){
                // Filter network card No
                val index = ipAddr.indexOf('%')
                if (index > 0) {
                    ipAddr = ipAddr.substring(0, index)
                }
            }

            return ipAddr
        }catch (e: SocketException){
            System.out.println("SocketException: NetworkInterface.getNetworkInterfaces() fail")
        }catch (e: NoSuchElementException){
            System.out.println( "NoSuchElementException:  networkInterfaces.nextElement() fail")
        }
        return null
    }

    /**
     * Check if it's "local address" or "link local address" or
     * "loopbackaddress"
     * @param ip address
     * @return result
     */
    private fun isReservedAddr(inetAddr: InetAddress) = inetAddr.isAnyLocalAddress || inetAddr.isLinkLocalAddress
            || inetAddr.isLoopbackAddress
}
