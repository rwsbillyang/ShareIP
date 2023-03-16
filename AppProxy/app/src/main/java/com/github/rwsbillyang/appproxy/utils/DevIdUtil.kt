package com.github.rwsbillyang.appproxy.utils

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import java.net.NetworkInterface
import java.util.*


object DevIdUtil {
    fun devId(context: Context): String?{
        return if (Build.VERSION.SDK_INT >= 26)
            Settings.System.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        else{
             if (Build.VERSION.SDK_INT >= 23) {
                 getMacByJavaAPI()?:getMacBySystemInterface(context)//先通过NetworkInterface获取，获取不到再通过原方法获取
            } else {
                 //23以下直接获取
                 getMacBySystemInterface(context)
            }
        }
    }

    fun getRandomStr(num: Int = 16): String {
        val base = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        val random = Random()
        val sb = StringBuffer()
        for (i in 0 until num) {
            val number = random.nextInt(base.length)
            sb.append(base[number])
        }
        return sb.toString()
    }

    //通过NetworkInterface获取
    private fun getMacByJavaAPI(): String? {
        try {
            val interfaces: Enumeration<NetworkInterface> = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val netInterface: NetworkInterface = interfaces.nextElement()
                if ("wlan0" == netInterface.name || "eth0" == netInterface.name) {
                    val addr: ByteArray = netInterface.hardwareAddress
                    if (addr.isEmpty()) {
                        return null
                    }
                    val buf = StringBuilder()
                    for (b in addr) {
                        buf.append(String.format("%02X:", b))
                    }
                    if (buf.isNotEmpty()) {
                        buf.deleteCharAt(buf.length - 1)
                    }
                    return buf.toString().lowercase(Locale.getDefault())
                }
            }
        } catch (e: Throwable) {
            Log.w("DevIdUtil", "getMacByJavaAPI: no permission: ${e.message}")
        }
        return null
    }

    //Android6.0之前（Build.VERSION.SDK_INT < 23）通过WifiInfo.getMacAddress()获取
    private fun getMacBySystemInterface(context: Context): String? {
        return try {
            val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val info = wifi.connectionInfo
            info.macAddress
        } catch (e: Throwable) {
            Log.w("DevIdUtil", "getMacBySystemInterface: no permission: ${e.message}")
            null
        }
    }
}
