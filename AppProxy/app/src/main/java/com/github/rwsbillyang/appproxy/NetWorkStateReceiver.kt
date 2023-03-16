package com.github.rwsbillyang.appproxy

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Build
import android.widget.Toast


class NetWorkStateReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        //检测API是不是小于23，因为到了API23之后getNetworkInfo(int networkType)方法被弃用
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            //获得ConnectivityManager对象
            val connMgr = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            //获取ConnectivityManager对象对应的NetworkInfo对象
            //获取WIFI连接的信息
            val wifiNetworkInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
            //获取移动数据连接的信息
            val dataNetworkInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)
            if (wifiNetworkInfo!!.isConnected && dataNetworkInfo!!.isConnected) {
                Toast.makeText(context, "WIFI已连接,移动数据已连接", Toast.LENGTH_SHORT).show()
            } else if (wifiNetworkInfo.isConnected && !dataNetworkInfo!!.isConnected) {
                Toast.makeText(context, "WIFI已连接,移动数据已断开", Toast.LENGTH_SHORT).show()
            } else if (!wifiNetworkInfo.isConnected && dataNetworkInfo!!.isConnected) {
                Toast.makeText(context, "WIFI已断开,移动数据已连接", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "WIFI已断开,移动数据已断开", Toast.LENGTH_SHORT).show()
            }

        } else {//API大于23时使用下面的方式进行网络监听
            val connMgr = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            //获取所有网络连接的信息
            val networks = connMgr.allNetworks
            //用于存放网络连接信息
            val sb = StringBuilder()
            //通过循环将网络信息逐个取出来
            for (i in networks.indices) {
                //获取ConnectivityManager对象对应的NetworkInfo对象
                val networkInfo = connMgr.getNetworkInfo(networks[i])
                sb.append(networkInfo!!.typeName + " connect is " + networkInfo.isConnected)
            }
            Toast.makeText(context, sb.toString(), Toast.LENGTH_SHORT).show()
        }
    }
}
