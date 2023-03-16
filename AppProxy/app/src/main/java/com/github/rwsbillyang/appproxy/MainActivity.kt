package com.github.rwsbillyang.appproxy


import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.VpnService
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log

import android.view.Menu
import android.view.MenuItem

import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.github.rwsbillyang.appproxy.socks5.ProxyController
import com.github.rwsbillyang.appproxy.utils.IPv4Util
import com.github.rwsbillyang.appproxy.vpn.MyVpnService
import com.github.rwsbillyang.proxy.ClientInfo
import com.github.rwsbillyang.proxy.peer.HttpUtil
import com.github.rwsbillyang.proxy.peer.IPv6Util
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json


class MainActivity : AppCompatActivity(), PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {
    companion object {
        const val TAG = "MainActivity"
        const val REQUEST_VPN = 1
        const val REQUEST_CERT = 2

        const val ONGOING_NOTIFICATION_ID = 1
    }
    private var startButton: Button? = null
    private var stopButton: Button? = null
    private var statusView: TextView? = null
    private var hostEditText: EditText? = null
    private val handler = Handler(Looper.getMainLooper())

    private var service: MyVpnService? = null
    private lateinit var controller: ProxyController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//        val toolbar = findViewById<Toolbar>(R.id.toolbar)
//        setSupportActionBar(toolbar)

        startButton = findViewById<Button>(R.id.start)?.apply{
            setOnClickListener{ startVpn() }
            isEnabled = true
        }
        stopButton = findViewById<Button>(R.id.stop)?.apply{
            setOnClickListener{ stopVpn() }
            isEnabled = false
        }
        statusView = findViewById(R.id.vpnStatus)

        initIpInfo()
        controller = ProxyController(this)
        controller.doWork()
    }

    private fun initIpInfo(){
        findViewById<TextView>(R.id.ipv4)?.apply{
            text = IPv6Util.getLocalIPAddress(false)
        }
        findViewById<TextView>(R.id.ipv6)?.apply{
            text = IPv6Util.getLocalIPAddress()
        }
        val natView = findViewById<TextView>(R.id.natIP)
        val addressView = findViewById<TextView>(R.id.address)

//        Thread {
//            Log.i(TAG, "doGet clientInfo")
//            val json = HttpUtil.doGet("$server/api/proxy/ip", null)
//            Log.i(TAG, "doGet return clientInfo: $json")
//
//            if(!json.isNullOrEmpty()){
//                val clientInfo = Json.decodeFromString<ClientInfo>(json)
//                natView?.text = clientInfo.remoteHost
//                addressView?.text = clientInfo.city
//            }
//        }.start()

        runBlocking(Dispatchers.IO) {
            launch {
                Log.i(TAG, "doGet clientInfo")
                val json = HttpUtil.doGet("${ProxyController.server}/api/proxy/ip", null)
                Log.i(TAG, "doGet return clientInfo: $json")

                if(!json.isNullOrEmpty()){
                    val clientInfo = Json.decodeFromString<ClientInfo>(json)
                    natView?.text = clientInfo.remoteHost
                    addressView?.text = clientInfo.city
                }
            }
        }
    }


    override fun onPreferenceStartFragment(
        caller: PreferenceFragmentCompat,
        pref: Preference
    ): Boolean {
        val args = pref.extras
        val fragment = supportFragmentManager.fragmentFactory.instantiate(
            classLoader,
            pref.fragment!!
        )
        fragment.arguments = args
        fragment.setTargetFragment(caller, 0)
        supportFragmentManager.beginTransaction()
            .replace(R.id.activity_settings, fragment)
            .addToBackStack(null)
            .commit()
        title = pref.title
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val item = menu.findItem(R.id.action_activity_settings)
        item.isEnabled = startButton?.isEnabled?:false
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_activity_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
            }
            R.id.action_show_about -> AlertDialog.Builder(this)
                .setTitle(getString(R.string.app_name) + versionName)
                .setMessage(R.string.app_name)
                .show()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private val versionName: String?
         get() {
            val packageManager = packageManager ?: return null
            return try {
                packageManager.getPackageInfo(packageName, 0).versionName
            } catch (e: PackageManager.NameNotFoundException) {
                null
            }
        }

    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, binder: IBinder) {
            val serviceBinder: MyVpnService.LocalBinder =
                binder as MyVpnService.LocalBinder
            service = serviceBinder.getService()
        }

        override fun onServiceDisconnected(className: ComponentName) {
            service = null
        }
    }


    override fun onResume() {
        super.onResume()
        startButton?.isEnabled = false
        stopButton?.isEnabled = false
        updateStatus()
        handler.post(statusRunnable)
        val intent = Intent(this, MyVpnService::class.java)
        bindService(intent, serviceConnection, BIND_AUTO_CREATE)
    }

    private val isRunning: Boolean
        get() = service != null && service!!.isRunning()

    private var statusRunnable: Runnable = object : Runnable {
        override fun run() {
            updateStatus()
            handler.post(this)
        }
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(statusRunnable)
        unbindService(serviceConnection)
    }

    fun updateStatus() {
        if (service == null) {
            return
        }
        startButton?.isEnabled = !isRunning
        stopButton?.isEnabled = isRunning
        hostEditText?.isEnabled = !isRunning
        statusView?.text =  getText(if(isRunning)R.string.running else R.string.stopped)
        statusView?.setTextColor(if(isRunning) Color.GREEN else Color.RED)
    }

    private fun startVpn() {
        val i = VpnService.prepare(this)
        if (i != null) {
            startActivityForResult(i, REQUEST_VPN)
        } else {
            onActivityResult(REQUEST_VPN, RESULT_OK, null)
        }
    }
    private fun stopVpn() {
        startButton?.isEnabled = true
        stopButton?.isEnabled = false
        MyVpnService.stop(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) {
            return
        }
        if (requestCode == REQUEST_VPN && parseAndSaveHostPort()) {
            startButton?.isEnabled = false
            stopButton?.isEnabled = true
            MyVpnService.start(this)
        }
    }


    private fun parseAndSaveHostPort(): Boolean {
        val hostPort = hostEditText!!.text.toString()
        if (!IPv4Util.isValidIPv4Address(hostPort)) {
            hostEditText!!.error = getString(R.string.enter_host)
            return false
        }
        val parts = hostPort.split(":").toTypedArray()
        var port = 0
        if (parts.size > 1) {
            try {
                port = parts[1].toInt()
            } catch (e: NumberFormatException) {
                hostEditText!!.error = getString(R.string.enter_host)
                return false
            }
        }
        val ipParts = parts[0].split("\\.").toTypedArray()
        val host = parts[0]
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val edit = prefs.edit()
        edit.putString(MyVpnService.PREF_PROXY_HOST, host)
        edit.putInt(MyVpnService.PREF_PROXY_PORT, port)
        edit.commit()
        return true
    }
}
