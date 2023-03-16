package com.github.rwsbillyang.appproxy


import android.app.Application
import androidx.preference.PreferenceManager

enum class VPNMode { ALLOW, DISALLOW}
enum class AppSortBy { APPNAME, PKGNAME }
enum class AppOrderBy { ASC, DESC }
enum class AppFilterBy { APPNAME, PKGNAME }

class MyApplication : Application() {
    companion object {
        private const val PREF_VPN_MODE = "pref_vpn_connection_mode"
        private val PREF_APP_KEY = arrayOf("pref_vpn_disallowed_application", "pref_vpn_allowed_application")
        lateinit var instance: MyApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    fun loadVPNMode(): VPNMode {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val vpnMode = sharedPreferences.getString(PREF_VPN_MODE, VPNMode.ALLOW.name)
        return VPNMode.valueOf(vpnMode!!)
    }

    fun storeVPNMode(mode: VPNMode) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val editor = prefs.edit()
        editor.putString(PREF_VPN_MODE, mode.name).apply()
        return
    }

    fun loadVPNApplication(mode: VPNMode): Set<String> {
        return PreferenceManager.getDefaultSharedPreferences(applicationContext).getStringSet(PREF_APP_KEY[mode.ordinal], null)?:HashSet()
    }

    fun storeVPNApplication(mode: VPNMode, set: Set<String?>?) {
        PreferenceManager.getDefaultSharedPreferences(applicationContext).edit().putStringSet(PREF_APP_KEY[mode.ordinal], set).apply()
        return
    }


}
