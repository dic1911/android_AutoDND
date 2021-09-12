package moe.dic1911.autodnd.data

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import moe.dic1911.autodnd.ui.main.MainFragment

object Storage {

    private val appListStorage = AppListStorage()
    private var initialized = false
    var setupStatus = 0
    private lateinit var pm: PackageManager
    private lateinit var prefs: SharedPreferences
    lateinit var prefs_str: MutableLiveData<ArrayList<String>>

    fun initForService(context: Context) {
        if (appListStorage.applist_dnd.size != 0) return
        prefs = context.getSharedPreferences("main", MODE_PRIVATE)
        val tmp = prefs.getString("dnd_enabled_apps", "")
        prefs_str = MutableLiveData()
        prefs_str.value = ArrayList(tmp!!.split(","))
    }

    fun initDNDList(context: Context) {
        pm = context.packageManager
        prefs = context.getSharedPreferences("main", MODE_PRIVATE)
        prefs_str = MutableLiveData()
        lateinit var prefs_str_tmp: ArrayList<String>

        // first use or data cleaned
        if (!prefs.contains("dnd_enabled_apps")) {
            prefs_str_tmp = ArrayList()
            prefs_str_tmp.add("jp.co.craftegg.band")
            prefs_str_tmp.add("com.sega.pjsekai")
            prefs_str_tmp.add("jp.co.taito.groovecoasterzero")
            prefs_str_tmp.add("klb.android.lovelive")
            prefs_str_tmp.add("klb.android.lovelive_en")
            prefs_str_tmp.add("moe.low.arc")
            val tmp = prefs_str_tmp.joinToString(separator = ",")
            Log.d("030_prefs", tmp)
            prefs.edit().putString("dnd_enabled_apps", tmp).apply()
        } else {
            val tmp = prefs.getString("dnd_enabled_apps", "")
            prefs_str_tmp = ArrayList(tmp!!.split(","))
            Log.d("030_prefs_load", tmp)
        }
        prefs_str.value = prefs_str_tmp
        initialized = true
    }

    fun appendToList(mode: Int, e: AppEntry) {
        when (mode) {
            0 -> {
                Log.d("030_append", e.app_pkgname!!)
                appListStorage.applist_dnd.add(e)
                val tmp = prefs_str.value
                tmp!!.add(e.app_pkgname!!)
                prefs_str.postValue(tmp)
            }
            1 -> {
                appListStorage.applist.add(e)
            }
        }
    }
    private fun appendToPrefs(s: String) {
        val orig = prefs.getString("dnd_enabled_apps", "")
        prefs.edit().putString("dnd_enabled_apps", "$orig,$s").apply()
    }

    fun getAppList(mode: Int): ArrayList<AppEntry>? {
        when (mode) {
            0 -> return appListStorage.applist_dnd
            1 -> return appListStorage.applist
        }
        return null
    }

    private fun isAutoDNDApp(pkg: String): Int {
        for (i in appListStorage.applist_dnd.indices) {
            if (pkg == appListStorage.applist_dnd[i].app_pkgname)
                return i
        }
        return -1
    }

    fun setAutoDNDApp(pkg: String): Boolean {
        val pos = isAutoDNDApp(pkg)
        if (pos != -1) {
            val tmp = ArrayList<String>()
            Log.d("030_remove", pkg)
            appListStorage.applist_dnd.removeAt(pos)
            for (e in appListStorage.applist_dnd) tmp.add(e.app_pkgname!!)
            prefs.edit().putString("dnd_enabled_apps", tmp.joinToString(",")).apply()
            prefs_str.postValue(tmp)
            return false
        }
        val e = AppEntry()
        val ai = pm.getApplicationInfo(pkg, 0)
        e.app_pkgname = pkg
        e.app_hname = ai.loadLabel(pm).toString()
        e.ic_app = pm.getApplicationIcon(ai)
        appendToList(0, e)
        appendToPrefs(pkg)
        return true
    }

}