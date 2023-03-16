package com.github.rwsbillyang.appproxy.socks5

import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
import androidx.preference.PreferenceManager
import androidx.work.*
import com.github.rwsbillyang.appproxy.MyApplication
import com.github.rwsbillyang.appproxy.utils.DevIdUtil
import com.github.rwsbillyang.proxy.Config
import com.github.rwsbillyang.proxy.Proxy
import com.github.rwsbillyang.proxy.peer.PeerAsServer
import com.github.rwsbillyang.proxy.peer.ProxyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


class ProxyController: ConnectivityManager.NetworkCallback() {
    companion object{
        const val TAG = "ProxyController"
        const val server = "https://zhike.niukid.com"

        const val workerName = "socks5Worker"
    }
    private val pref: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(MyApplication.instance.applicationContext)
    val proxyManager = ProxyManager(getDevId(), loadProxyList(), loadConfig()).apply{
        onGetProxyList { saveProxyList(it) }
        onServerConfigUpdate { saveConfig(it) }
    }
    val socks5Server = PeerAsServer(proxyManager)

    fun doWork(){
        join()
        startSocks5()
    }



    private fun join(){
        runBlocking(Dispatchers.IO) {
            launch {
                val ret = proxyManager.joinByHttp("$server/api/proxy/join")
                if(!ret) Log.w(TAG,"fail to join")
            }
        }
    }

    private fun startSocks5(){
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val socks5WorkRequest =
            OneTimeWorkRequestBuilder<Socks5Worker>()
                .setConstraints(constraints)
                .setInputData(workDataOf("socks5Server" to socks5Server))
                .build()

        WorkManager.getInstance(MyApplication.instance)
            .enqueueUniqueWork(workerName, ExistingWorkPolicy.KEEP, socks5WorkRequest)
    }
    override fun onAvailable(network: Network) {
        super.onAvailable(network)
    }

    override fun onUnavailable() {
        super.onUnavailable()
        WorkManager.getInstance(MyApplication.instance).cancelUniqueWork(workerName)
    }
    override fun onCapabilitiesChanged(
        network: Network,
        networkCapabilities: NetworkCapabilities
    ) {
        super.onCapabilitiesChanged(network, networkCapabilities)
        Log.i(TAG,"onCapabilitiesChanged")
        checkNetworkCapabilities(networkCapabilities)
    }
    override fun onLost(network: Network) {
        super.onLost(network)

        val cm = MyApplication.instance.getSystemService(ConnectivityManager::class.java)
        if (cm != null) {
            val activeNetwork: Network = cm.activeNetwork ?:return //连接不到可用网络

            val networkCapabilities: NetworkCapabilities = cm.getNetworkCapabilities(activeNetwork)
            checkNetworkCapabilities(networkCapabilities)
        }
    }


    //判断当前网络连接情况
    private fun checkNetworkCapabilities(networkCapabilities: NetworkCapabilities) {
        // 表明网络连接成功
        if (networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI_AWARE)
            ) {
                // 使用WI-FI
                Log.d(TAG, "WIFI network")
            } else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            ) {
                // 使用蜂窝网络
                Log.d(TAG, "mobile network")
            } else {
                // 未知网络，包括蓝牙、VPN、LoWPAN
                Log.d(TAG, "unknown network")
            }
        } else {
            //网络连接失败
        }
    }

    private fun getDevId(): String{
        var devId = pref.getString("devId", null)
        if(devId == null){
            devId = DevIdUtil.devId(MyApplication.instance)
            if(devId == null){
                Log.w("ProxyController", "fail to get device id, generate one")
                devId = DevIdUtil.getRandomStr(24)
            }
        }
        pref.edit().putString("devId", devId).apply()

        return devId
    }



    private fun loadProxyList() = pref.getString("proxies", null)?.let { Json.decodeFromString<List<Proxy>>(it) }
    private fun saveProxyList(list: List<Proxy>) = pref.edit().putString("proxies", Json.encodeToString(list)).apply()
    private fun loadConfig() = pref.getString("config", null)?.let { Json.decodeFromString<Config>(it) }
    private fun saveConfig(config: Config) = pref.edit().putString("config", Json.encodeToString(config)).apply()
}
