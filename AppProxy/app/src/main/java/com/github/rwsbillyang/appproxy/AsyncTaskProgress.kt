package com.github.rwsbillyang.appproxy

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.util.Log
import androidx.preference.Preference
import java.util.*
import kotlin.Comparator

class ProgressPreference(context: Context) : Preference(context) {
    init {
        layoutResource = R.layout.preference_progress
    }
}
/*
* AsyncTask
* https://developer.android.com/reference/android/os/AsyncTask
* Deprecated in API level R
* */
class AsyncTaskProgress(private val packageFragment: PackageListFragment) :
    ProgressTask<String, String, List<PackageInfo>>() {
    companion object{
       const val TAG = "AsyncTaskProgress"
    }
    override fun onPreExecute() {
        packageFragment.activity?.let{
            val a = it
            packageFragment.mFilterPreferenceScreen?.addPreference(ProgressPreference(a))
        }
        Log.d(TAG, "onPreExecute")
    }

    override fun doInBackground(vararg params: String): List<PackageInfo> {
        return filterPackages(
            packageFragment.searchFilter,
            packageFragment.appFilterBy,
            packageFragment.appOrderBy,
            packageFragment.appSortBy
        )
    }

    private fun filterPackages(
        filter: String?,
        filterBy: AppFilterBy,
        orderBy: AppOrderBy,
        sortBy: AppSortBy
    ): List<PackageInfo> {
        val context: Context = MyApplication.instance.applicationContext
        val pm = context.packageManager
        val installedPackages = pm.getInstalledPackages(PackageManager.GET_META_DATA).filterNot { it.packageName == context.packageName }
        val f = filter?.trim()?.lowercase(Locale.getDefault())
        val list = if(!f.isNullOrEmpty()){
            when(filterBy){
                AppFilterBy.APPNAME -> installedPackages.filter { it.applicationInfo.loadLabel(pm).toString().lowercase().contains(f) }.toMutableList()
                AppFilterBy.PKGNAME -> installedPackages.filter { it.packageName.lowercase().contains(f) }.toMutableList()
            }
        }else installedPackages.toMutableList()

        Log.d(TAG, "filterPackages: installed packages: ${installedPackages.size}")
        list.sortWith(Comparator { o1, o2 ->
            var t1 = ""
            var t2 = ""
            when (sortBy) {
                AppSortBy.APPNAME -> {
                    t1 = o1.applicationInfo.loadLabel(pm).toString()
                    t2 = o2.applicationInfo.loadLabel(pm).toString()
                }
                AppSortBy.PKGNAME -> {
                    t1 = o1.packageName
                    t2 = o2.packageName
                }
            }
            if (AppOrderBy.ASC == orderBy) t1.compareTo(t2) else t2.compareTo(
                t1
            )
        })

        val installedPackageMap: MutableMap<String, Boolean> = HashMap()
        for (pi in list) {
            if (isCancelled) continue
            //if(pi.packageName == MyApplication.instance.packageName) continue
            val checked = packageFragment.mAllPackageInfoMap[pi.packageName]?:false
            installedPackageMap[pi.packageName] = checked
        }
        packageFragment.mAllPackageInfoMap.clear()
        packageFragment.mAllPackageInfoMap.putAll(installedPackageMap)
        return list
    }

    override fun onPostExecute(result: List<PackageInfo>) {
        Log.d(TAG, "onPostExecute")

        val pm = MyApplication.instance.applicationContext.packageManager
        packageFragment.mFilterPreferenceScreen?.removeAll()
        for (pi in result) {
            // exclude self package
//            if (pi.packageName == MyApplication.instance.packageName) {
//                continue
//            }
//
//            val t2 = packageFragment.searchFilter.trim()
//            val t1 = when (packageFragment.appFilterBy) {
//                AppFilterBy.APPNAME -> pi.applicationInfo.loadLabel(pm).toString()
//                AppFilterBy.PKGNAME -> pi.packageName
//            }
//            if (t2.isEmpty() || t1.lowercase(Locale.getDefault()).contains(t2.lowercase(Locale.getDefault()))
//            ) {
                val preference = packageFragment.buildPackagePreferences(pm, pi)
                if (preference != null &&  packageFragment.mFilterPreferenceScreen != null) {
                    Log.d(TAG, "onPostExecute: addPreference")
                    packageFragment.mFilterPreferenceScreen!!.addPreference(preference)
                }
            //}
        }
        return
    }

    override fun onCancelled() {
        super.onCancelled()
        Log.d(TAG, "onCancelled")
        packageFragment.mAllPackageInfoMap.clear()
        packageFragment.mFilterPreferenceScreen?.removeAll()
        return
    }
}
