package com.github.rwsbillyang.appproxy.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import java.net.InetAddress


object NetworkUtil {

    fun getDefaultDNS(context: Context): List<String> {
        var dns1: String? = null
        var dns2: String? = null
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val an = cm.activeNetwork
            if (an != null) {
                val lp = cm.getLinkProperties(an)
                if (lp != null) {
                    val dns: List<InetAddress> = lp.dnsServers
                    if (dns.isNotEmpty()) dns1 = dns[0].hostAddress
                    if (dns.size > 1) dns2 = dns[1].hostAddress
                }
            }
        }
        val listDns: MutableList<String> = ArrayList()
        listDns.add(if(dns1.isNullOrEmpty()) "223.5.5.5" else dns1)
        listDns.add(if (dns2.isNullOrEmpty()) "223.6.6.6" else dns2)
//        listDns.add(if(dns1.isNullOrEmpty()) "8.8.8.8" else dns1)
//        listDns.add(if (dns2.isNullOrEmpty()) "8.8.4.4" else dns2)
        return listDns
    }

    /**
     * 判断网络是否连接
     */
    fun isNetworkConnected(context: Context?): Boolean {
        if (context != null) {
            val mConnectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = mConnectivityManager.activeNetwork ?: return false
            val status = mConnectivityManager.getNetworkCapabilities(network)?: return false
            if (status.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                return true
            }
        }
        return false
    }

    /**
     * 判断是否是WiFi连接
     */
    fun isWifiConnected(context: Context?): Boolean {
        if (context != null) {
            val mConnectivityManager = context
                .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = mConnectivityManager.activeNetwork ?: return false
            val status = mConnectivityManager.getNetworkCapabilities(network)
                ?: return false
            if (status.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                return true
            }
        }
        return false
    }

    /**
     * 判断是否是数据网络连接
     */
    fun isMobileConnected(context: Context?): Boolean {
        if (context != null) {
            val mConnectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = mConnectivityManager.activeNetwork ?: return false
            val status = mConnectivityManager.getNetworkCapabilities(network)?: return false

            //status.transportInfo
            if (status.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                return true
            }
        }
        return false
    }
}
