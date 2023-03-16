package com.github.rwsbillyang.appproxy.vpn


import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.*

import android.util.Log
import androidx.annotation.RequiresApi
import androidx.preference.PreferenceManager
import com.github.rwsbillyang.appproxy.MainActivity
import com.github.rwsbillyang.appproxy.MainActivity.Companion.ONGOING_NOTIFICATION_ID
import com.github.rwsbillyang.appproxy.MyApplication
import com.github.rwsbillyang.appproxy.R
import com.github.rwsbillyang.appproxy.VPNMode
import com.github.rwsbillyang.appproxy.utils.NetworkUtil
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.channels.DatagramChannel


class MyVpnService : VpnService(){
    companion object {
        private const val TAG = "MyVpnService"

        const val PREF_PROXY_HOST = "pref_proxy_host"
        const val PREF_PROXY_PORT = "pref_proxy_port"
        const val PREF_RUNNING = "pref_running"

        private const val ACTION_START = "start"
        private const val ACTION_STOP = "stop"


        fun start(context: Context) {
            val intent = Intent(context, MyVpnService::class.java)
            intent.action = ACTION_START
            context.startService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, MyVpnService::class.java)
            intent.action = ACTION_STOP
            context.startService(intent)
        }
    }
    private var vpn: ParcelFileDescriptor? = null

    // Binder given to clients
    private val binder = LocalBinder()
    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    inner class LocalBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods
        fun getService(): MyVpnService = this@MyVpnService
    }
    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "Received $intent")

        if (intent == null) {
            return START_STICKY
        }
        if (ACTION_START == intent.action) {
            if (NetworkUtil.isNetworkConnected(this) && vpn == null) {
                // If the notification supports a direct reply action, use
                // PendingIntent.FLAG_MUTABLE instead.
                val pendingIntent: PendingIntent =
                    Intent(this, MainActivity::class.java).let { notificationIntent ->
                        PendingIntent.getActivity(this, 0, notificationIntent,
                            PendingIntent.FLAG_IMMUTABLE)
                    }
                val appName = getText(R.string.app_name)
                val statusText = if(isRunning())  "Running" else "Stopped"
                val notification: Notification = Notification.Builder(this, "100")
                    .setContentTitle(appName)
                    .setContentText(statusText)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentIntent(pendingIntent)
                    .setTicker(appName)
                    .build()

                    // Notification ID cannot be 0.
                    startForeground(ONGOING_NOTIFICATION_ID, notification)

                    startVpn()
            }else{
                Log.w(TAG, "no network or vpn is null")
            }
        }
        if (ACTION_STOP == intent.action) {
            stopVpn()
            stopForeground(true)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        Log.i(TAG, "Destroy")
        try {
            if (vpn != null) {
                vpn = null
            }
        } catch (ex: Throwable) {
            Log.e(TAG, ex.message?:ex.stackTraceToString())
        }

        super.onDestroy()
    }
    fun isRunning(): Boolean {
        return vpn != null
    }



    private fun startVpn() {
        if (vpn == null) {
            vpn = try {
                getBuilder().establish()
            } catch (ex: SecurityException) {
                Log.e(TAG, "SecurityException: ${ex.message}")
                null
            } catch (ex: Exception) {
                Log.e(TAG, ex.message?:ex.stackTraceToString())
                null
            }
            checkNotNull(vpn) { getString(R.string.msg_start_failed) }

            val mInterface = vpn!!
            //b. Packets to be sent are queued in this input stream.
            val `in` = FileInputStream(mInterface.fileDescriptor)
            //b. Packets received need to be written to this output stream.
            val out = FileOutputStream(mInterface.fileDescriptor)

            //c. The UDP channel can be used to pass/get ip package to/from server
            val tunnel: DatagramChannel = DatagramChannel.open()

            // Connect to the server, localhost is used for demonstration only.
            tunnel.connect(InetSocketAddress("127.0.0.1", 8087))

            //d. Protect this socket, so package send by it will not be feedback to the vpn service.
            protect(tunnel.socket())

        }
    }

    private fun stopVpn() {
        if (vpn != null) {
            //stopNative(vpn)
            try {
                vpn!!.close()
            } catch (ex: IOException) {
                Log.e(TAG, "IOException: ${ex.message}")
            }
            vpn = null
        }

    }

    override fun onRevoke() {
        Log.i(TAG, "Revoke")
        stopVpn()
        super.onRevoke()
    }




    private fun getBuilder(): Builder {
        val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        val builder = Builder()

        //https://github.com/asdzheng/vpnservices
        //VPN连接的名字，它将会在系统管理的与VPN连接相关的通知栏和对话框中显示出来
        builder.setSession(getString(R.string.app_name))

        // 设置虚拟网络端口tun0的IP地址，例如这个设置为：26.26.26.1，到时从 tun0读取出来的ip包，
        // 源ip就是26.26.26.1。这个ip设置最好选择这样一个不常见的IP，防止和目前已经有的IP地址冲突
        val vpn4 = prefs.getString("vpn4", "10.1.10.1")
        builder.addAddress(vpn4!!, 32)
        val vpn6 = prefs.getString("vpn6", "fd00:1:fd00:1:fd00:1:fd00:1")
        builder.addAddress(vpn6!!, 128)


        builder.addAddress("fd00:1:fd00:1:fd00:1:fd00:1", 128)

        //addRoute("198.18.0.0", 16), 那意思就是只有目标地址是198.18...开头的ip段会走代理，其他ip端还是直连；
        // 那么如果想所有ip都走代理呢，那就设置"0.0.0.0/0"就行(ipv6使用::/0)，意思是所有ip都走代理;
        builder.addRoute("0.0.0.0", 0)
        builder.addRoute("0:0:0:0:0:0:0:0", 0)

        val dnsList = NetworkUtil.getDefaultDNS(MyApplication.instance.applicationContext)

        for (dns in dnsList) {
            Log.i(TAG, "default DNS:$dns")
            builder.addDnsServer(dns)
        }

        val app = MyApplication.instance
        val notFoundPackages = mutableSetOf<String>()
        val apps = app.loadVPNApplication(app.loadVPNMode())
        Log.d(TAG, "apps size:" + apps.size)

        if (app.loadVPNMode() === VPNMode.DISALLOW) {
            apps.forEach {
                try {
                    builder.addDisallowedApplication(it)
                } catch (e: PackageManager.NameNotFoundException){
                    notFoundPackages.add(it)
                }
            }
            builder.addDisallowedApplication(packageName)
        } else {
            apps.forEach {
                try {
                    builder.addAllowedApplication(it)
                } catch (e: PackageManager.NameNotFoundException){
                    notFoundPackages.add(it)
                }
            }
        }

        //builder.setMetered(true) //如果是计量网络就会优化请求数据量，例如一般国内应用会在Wifi网络环境下后台自动下载升级apk，如果是使用isActiveNetworkMetered的话来判断是否可以使用大数据量下载，vpn这个设置就会影响isActiveNetworkMetered的结果。

        if(notFoundPackages.isNotEmpty()){
            apps.toMutableSet().removeAll(notFoundPackages)
            app.storeVPNApplication(app.loadVPNMode(), apps)
        }

        return builder
    }

}
