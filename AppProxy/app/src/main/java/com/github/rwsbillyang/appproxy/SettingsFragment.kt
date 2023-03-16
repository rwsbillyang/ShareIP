package com.github.rwsbillyang.appproxy

import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import java.util.HashSet


class SettingsFragment : PreferenceFragmentCompat(),
    Preference.OnPreferenceClickListener {

    companion object {
        const val VPN_CONNECTION_MODE = "vpn_connection_mode"
        const val VPN_DISALLOWED_APPLICATION_LIST = "vpn_disallowed_application_list"
        const val VPN_ALLOWED_APPLICATION_LIST = "vpn_allowed_application_list"
        const val VPN_CLEAR_ALL_SELECTION = "vpn_clear_all_selection"
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences)
        setHasOptionsMenu(true)

        /* Allowed / Disallowed Application */
        val prefPackage = findPreference(VPN_CONNECTION_MODE) as? ListPreference
        val prefDisallow = findPreference(VPN_DISALLOWED_APPLICATION_LIST) as? PreferenceScreen
        val prefAllow = findPreference(VPN_ALLOWED_APPLICATION_LIST) as? PreferenceScreen
        val clearAllSelection = findPreference(VPN_CLEAR_ALL_SELECTION) as? PreferenceScreen

        if(clearAllSelection != null) clearAllSelection.onPreferenceClickListener = this

        if(prefPackage != null){
            prefPackage.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { preference, value ->
                    if (preference is ListPreference) {
                        val index = preference.findIndexOfValue(value as String)
                        if(prefDisallow != null)
                            prefDisallow.isEnabled = index == VPNMode.DISALLOW.ordinal

                        if(prefAllow != null)
                            prefAllow.isEnabled = index == VPNMode.ALLOW.ordinal

                        // Set the summary to reflect the new value.
                        preference.setSummary(if (index >= 0) preference.entries[index] else null)
                        val mode: VPNMode = VPNMode.values()[index]
                        MyApplication.instance.storeVPNMode(mode)
                    }
                    true
                }
            prefPackage.summary = prefPackage.entry
        }


        if(prefDisallow != null) prefDisallow.isEnabled = VPNMode.DISALLOW.name == prefPackage?.value
        if(prefAllow != null) prefAllow.isEnabled = VPNMode.ALLOW.name == prefPackage?.value
        updateMenuItem()
    }

    private fun updateMenuItem() {
        val prefDisallow = findPreference(VPN_DISALLOWED_APPLICATION_LIST) as? PreferenceScreen
        val prefAllow = findPreference(VPN_ALLOWED_APPLICATION_LIST) as? PreferenceScreen
        val countDisallow = MyApplication.instance.loadVPNApplication(VPNMode.DISALLOW).size
        val countAllow = MyApplication.instance.loadVPNApplication(VPNMode.ALLOW).size
        if(prefDisallow != null) prefDisallow.title = getString(R.string.pref_header_disallowed_application_list) +  " ($countDisallow)"
        if(prefAllow != null) prefAllow.title = getString(R.string.pref_header_allowed_application_list) + " ($countAllow)"
    }

    /*
     * https://developer.android.com/guide/topics/ui/settings/organize-your-settings
     */
    override fun onPreferenceClick(preference: Preference): Boolean {
        when (preference.key) {
            VPN_DISALLOWED_APPLICATION_LIST, VPN_ALLOWED_APPLICATION_LIST -> {}
            VPN_CLEAR_ALL_SELECTION -> activity?.let {
                androidx.appcompat.app.AlertDialog.Builder(it)
                    .setTitle(getString(R.string.title_activity_settings))
                    .setMessage(getString(R.string.pref_dialog_clear_all_application_msg))
                    .setPositiveButton("OK"
                    ) { _, _ ->
                        val set: Set<String> = HashSet()
                        MyApplication.instance.storeVPNApplication(VPNMode.ALLOW, set)
                        MyApplication.instance.storeVPNApplication(VPNMode.DISALLOW, set)
                        updateMenuItem()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
        return false
    }
}
