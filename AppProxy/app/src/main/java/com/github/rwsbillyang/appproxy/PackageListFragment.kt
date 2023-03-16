package com.github.rwsbillyang.appproxy

import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.SearchView
import androidx.preference.*
import java.util.HashMap
import java.util.HashSet

class DisallowedPackageListFragment : PackageListFragment(VPNMode.DISALLOW)
class AllowedPackageListFragment : PackageListFragment(VPNMode.ALLOW)

open class PackageListFragment(private val mode: VPNMode) :
    PreferenceFragmentCompat(), SearchView.OnQueryTextListener, SearchView.OnCloseListener {
    companion object {
        private const val PREF_VPN_APPLICATION_ORDERBY = "pref_vpn_application_app_orderby"
        private const val PREF_VPN_APPLICATION_FILTERBY = "pref_vpn_application_app_filterby"
        private const val PREF_VPN_APPLICATION_SORTBY = "pref_vpn_application_app_sortby"
    }

    val mAllPackageInfoMap: MutableMap<String, Boolean> = HashMap()
    private var task: AsyncTaskProgress? = null
    var appSortBy: AppSortBy = AppSortBy.APPNAME
    var appOrderBy: AppOrderBy = AppOrderBy.DESC
    var appFilterBy: AppFilterBy = AppFilterBy.APPNAME
    var mFilterPreferenceScreen: PreferenceScreen? = null

    var searchFilter: String? = null
    private var searchView: SearchView? = null



    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setHasOptionsMenu(true)
        mFilterPreferenceScreen = activity?.let {
            preferenceManager.createPreferenceScreen(
                it
            )
        }
        preferenceScreen = mFilterPreferenceScreen

        task = AsyncTaskProgress(this)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.menu_search, menu)
        //MenuCompat.setGroupDividerEnabled(menu, true);
        val menuSearch = menu.findItem(R.id.menu_search_item)
        searchView = menuSearch.actionView as SearchView
        if(searchView != null){
            searchView!!.setOnQueryTextListener(this)
            searchView!!.setOnCloseListener(this)
            searchView!!.isSubmitButtonEnabled = false
        }

        when (appOrderBy) {
            AppOrderBy.ASC -> {
                val menuItem = menu.findItem(R.id.menu_sort_order_asc)
                menuItem.isChecked = true
            }
            AppOrderBy.DESC -> {
                val menuItem = menu.findItem(R.id.menu_sort_order_desc)
                menuItem.isChecked = true
            }
        }

        when (appFilterBy) {
            AppFilterBy.APPNAME -> {
                val menuItem = menu.findItem(R.id.menu_filter_app_name)
                menuItem.isChecked = true
            }
            AppFilterBy.PKGNAME -> {
                val menuItem = menu.findItem(R.id.menu_filter_pkg_name)
                menuItem.isChecked = true
            }
        }
        when (appSortBy) {
            AppSortBy.APPNAME -> {
                val menuItem = menu.findItem(R.id.menu_sort_app_name)
                menuItem.isChecked = true
            }
            AppSortBy.PKGNAME -> {
                val menuItem = menu.findItem(R.id.menu_sort_pkg_name)
                menuItem.isChecked = true
            }
        }
    }



    private fun filter(
        f: String? = null,
        filterBy: AppFilterBy = appFilterBy,
        orderBy: AppOrderBy = appOrderBy,
        sortBy: AppSortBy = appSortBy
    ) {
        if (f != null) {
            searchFilter = f
        }

        appFilterBy = filterBy
        appOrderBy = orderBy
        appSortBy = sortBy
        val selected = allSelectedPackageSet
        storeSelectedPackageSet(selected)
        removeAllPreferenceScreen()
        if(task != null){
            if (task!!.status === ProgressTask.Status.PENDING) {
                task!!.execute()
            }
        }else{
            task = AsyncTaskProgress(this)
            task!!.execute()
        }

        //            this.filterPackagesPreferences(filter, sortBy, orderBy);
    }

    override fun onPause() {
        super.onPause()
        if (task != null) {
            task!!.cancel(true)
            task = null
        }
        val selected = allSelectedPackageSet
        storeSelectedPackageSet(selected)
        val prefs = PreferenceManager.getDefaultSharedPreferences(
            MyApplication.instance.applicationContext
        )
        val edit = prefs.edit()
        edit.putString(PREF_VPN_APPLICATION_ORDERBY, appOrderBy.name)
        edit.putString(PREF_VPN_APPLICATION_FILTERBY, appFilterBy.name)
        edit.putString(PREF_VPN_APPLICATION_SORTBY, appSortBy.name)
        edit.apply()
    }

    override fun onResume() {
        super.onResume()
        val loadMap = MyApplication.instance.loadVPNApplication(mode)

        for (pkgName in loadMap) {
            mAllPackageInfoMap[pkgName] = loadMap.contains(pkgName)
        }
        val prefs = PreferenceManager.getDefaultSharedPreferences(
            MyApplication.instance.applicationContext
        )
        val appOrderBy =
            prefs.getString(PREF_VPN_APPLICATION_ORDERBY, AppOrderBy.ASC.name)
        val appFilterBy = prefs.getString(
            PREF_VPN_APPLICATION_FILTERBY,
            AppSortBy.APPNAME.name
        )
        val appSortBy = prefs.getString(PREF_VPN_APPLICATION_SORTBY, AppSortBy.APPNAME.name)
        this.appOrderBy = appOrderBy?.let { AppOrderBy.valueOf(it) }?:AppOrderBy.DESC

        this.appFilterBy = appFilterBy?.let { AppFilterBy.APPNAME }?:AppFilterBy.APPNAME
        this.appSortBy = appSortBy?.let{ AppSortBy.valueOf(appSortBy) }?: AppSortBy.APPNAME

        filter()
    }

    private fun removeAllPreferenceScreen() {
        mFilterPreferenceScreen?.removeAll()
    }

    fun buildPackagePreferences(pm: PackageManager, pi: PackageInfo): Preference? {
        val prefCheck = activity?.let { CheckBoxPreference(it) }
        if(prefCheck != null){
            prefCheck.icon = pi.applicationInfo.loadIcon(pm)
            prefCheck.title = pi.applicationInfo.loadLabel(pm).toString()
            prefCheck.summary = pi.packageName
            prefCheck.isChecked =  mAllPackageInfoMap[pi.packageName]?:false

            val click = Preference.OnPreferenceClickListener {
                mAllPackageInfoMap[prefCheck.summary.toString()] = prefCheck.isChecked
                false
            }
            prefCheck.onPreferenceClickListener = click
        }

        return prefCheck
    }

    private val filterSelectedPackageSet: MutableSet<String>
        get() {
            val selected: MutableSet<String> = HashSet()
            for (i in 0 until (mFilterPreferenceScreen?.preferenceCount?:0)) {
                val pref = mFilterPreferenceScreen?.getPreference(i)
                if (pref is CheckBoxPreference) {
                    if (pref.isChecked) {
                        selected.add(pref.summary.toString())
                    }
                }
            }
            return selected
        }

    private fun setSelectedPackageSet(selected: Set<String>) {
        for (i in 0 until (mFilterPreferenceScreen?.preferenceCount?:0)) {
            val pref = mFilterPreferenceScreen?.getPreference(i)
            if (pref is CheckBoxPreference) {
                if (selected.contains(pref.summary)) {
                    pref.isChecked = true
                }
            }
        }
    }

    private fun clearAllSelectedPackageSet() {
        val selected: Set<String> = filterSelectedPackageSet
        for (value in mAllPackageInfoMap.entries) {
            if (value.value) {
                value.setValue(false)
            }
        }
    }

    private val allSelectedPackageSet: Set<String>
        get() {
            val selected = filterSelectedPackageSet
            for (e in mAllPackageInfoMap) {
                if (e.value) {
                    selected.add(e.key)
                }
            }
            return selected
        }

    private fun storeSelectedPackageSet(set: Set<String>) {
        MyApplication.instance.storeVPNMode(mode)
        MyApplication.instance.storeVPNApplication(mode, set)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                startActivity(Intent(activity, SettingsActivity::class.java))
                return true
            }
            R.id.menu_sort_order_asc -> {
                item.isChecked = !item.isChecked
                filter(null, appFilterBy, AppOrderBy.ASC, appSortBy)
            }
            R.id.menu_sort_order_desc -> {
                item.isChecked = !item.isChecked
                filter(null, appFilterBy, AppOrderBy.DESC, appSortBy)
            }
            R.id.menu_filter_app_name -> {
                item.isChecked = !item.isChecked
                appFilterBy = AppFilterBy.APPNAME
            }
            R.id.menu_filter_pkg_name -> {
                item.isChecked = !item.isChecked
                appFilterBy = AppFilterBy.PKGNAME
            }
            R.id.menu_sort_app_name -> {
                item.isChecked = !item.isChecked
                filter(null, appFilterBy, appOrderBy, AppSortBy.APPNAME)
            }
            R.id.menu_sort_pkg_name -> {
                item.isChecked = !item.isChecked
                filter(null, appFilterBy, appOrderBy, AppSortBy.PKGNAME)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        searchView!!.clearFocus()
        return if (!query.trim { it <= ' ' }.isEmpty()) {
            filter(query)
            true
        } else {
            filter("")
            true
        }
    }

    override fun onQueryTextChange(newText: String): Boolean {
        return false
    }

    override fun onClose(): Boolean {
        val selected = allSelectedPackageSet
        storeSelectedPackageSet(selected)
        filter("")
        return false
    }
}
