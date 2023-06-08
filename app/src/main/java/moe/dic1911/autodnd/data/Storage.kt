package moe.dic1911.autodnd.data

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import androidx.lifecycle.MutableLiveData
import rikka.shizuku.Shizuku

object Storage {

    private val appListStorage = AppListStorage()
    var initialized: MutableLiveData<Boolean> = MutableLiveData(false)
    var setupStatus = 0
    var shizukuMode = false
    private lateinit var pm: PackageManager
    private lateinit var prefs: SharedPreferences
    lateinit var prefs_str: MutableLiveData<ArrayList<String>>

    fun initForService(context: Context) {
        if (appListStorage.applist_dnd.size != 0) return
        prefs = context.getSharedPreferences("main", MODE_PRIVATE)
        val tmp = prefs.getString("dnd_enabled_apps", "")
        prefs_str = MutableLiveData()
        prefs_str.value = ArrayList(tmp!!.split(","))
        shizukuMode = prefs.getBoolean("shizuku", false)
        Log.d("030_dnd_shizuku", shizukuMode.toString())
    }

    fun initDNDList(context: Context) {
        initialized.postValue(false)
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

        if (getAppList(1)!!.size != 0)
            clearList()

        val pkgs = pm.getInstalledPackages(0)
        Log.d("030_pkg", "pkgs.size=${pkgs.size}")
        for (i in pkgs.indices) {
            val p = pkgs[i]
            val e = AppEntry()
            if (p!!.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0) {
                e.app_hname = p.applicationInfo.loadLabel(pm).toString()
                e.app_pkgname = p.packageName
                e.ic_app = p.applicationInfo.loadIcon(pm)
                appendToList(1, e)
                if (prefs_str.value!!.contains(p.packageName))
                    appendToList(0, e)
            } else {
                e.app_hname = p.applicationInfo.loadLabel(pm).toString()
                e.app_pkgname = p.packageName
                e.ic_app = p.applicationInfo.loadIcon(pm)
                appendToList(2, e)
                if (prefs_str.value!!.contains(p.packageName))
                    appendToList(0, e)
            }
            sortList(false)
        }

        initialized.postValue(true)
    }

    fun setShizuku(v: Boolean) {
        prefs.edit().putBoolean("shizuku", v).apply()
        shizukuMode = v
    }

    fun checkPermission(code: Int): Boolean {
        if (Shizuku.isPreV11()) {
            // Pre-v11 is unsupported
            return false
        }
        val granted = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        return if (!granted && !prefs.contains("shizuku")) {
            Shizuku.requestPermission(code)
            false
        } else {
            setShizuku(granted)
            granted
        }
    }

    fun clearList() {
        appListStorage.applist.clear()
        appListStorage.applist_dnd.clear()
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
            2 -> {
                appListStorage.applist_sys.add(e)
            }
        }
    }

    fun sortList(dndOnly: Boolean) {
        if (!dndOnly)
            appListStorage.applist.sortWith(compareBy{ it.app_hname })
        appListStorage.applist_dnd.sortWith(compareBy{ it.app_hname })
    }

    private fun appendToPrefs(s: String) {
        val orig = prefs.getString("dnd_enabled_apps", "")
        prefs.edit().putString("dnd_enabled_apps", "$orig,$s").apply()
    }

    fun getAppList(mode: Int): ArrayList<AppEntry>? {
        when (mode) {
            0 -> return appListStorage.applist_dnd
            1 -> return appListStorage.applist
            2 -> return appListStorage.applist_sys
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
        sortList(true)
        return true
    }

    fun onShizukuRequestPermissionsResult(requestCode: Int, grantResult: Int) {
        val granted = grantResult == PackageManager.PERMISSION_GRANTED
        // Do stuff based on the result and the request code
        setShizuku(granted)
    }

}