package moe.dic1911.autodnd

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityEvent.TYPE_VIEW_CLICKED
import android.view.accessibility.AccessibilityEvent.TYPE_VIEW_FOCUSED
import android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
import android.view.accessibility.AccessibilityEvent.eventTypeToString
import moe.dic1911.autodnd.data.Storage
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Calendar


class DNDAccessibilityService : AccessibilityService() {
    private var appList: ArrayList<String> = ArrayList()
    private val blacklist: ArrayList<String> = ArrayList()
    private val info = AccessibilityServiceInfo()
    private val eventTypes = TYPE_VIEW_FOCUSED or TYPE_VIEW_CLICKED or TYPE_WINDOW_STATE_CHANGED
    private var curApp = ""
    private var state = 0
    private var bakNotiState = NotificationManager.INTERRUPTION_FILTER_ALL
    private var bakRingMode = AudioManager.RINGER_MODE_VIBRATE
    private var lastEvtTime: Long = 0

    override fun onInterrupt() {}

    override fun onServiceConnected() {
        super.onServiceConnected()
        info.eventTypes = eventTypes
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.notificationTimeout = 100
        this.serviceInfo = info

        Storage.initForService(applicationContext)
        blacklist.add("android")
        blacklist.add("com.android.systemui")

        bakRingMode = (getSystemService(Context.AUDIO_SERVICE) as AudioManager).ringerMode

        val recv_ringer: BroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val notiMan = (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                val dnd = notiMan.currentInterruptionFilter
                if (state == 1 || dnd != 1) {
                    Log.d("030_recv_ring", "Either we're running apps in list or DND is on/unknown")
                    return
                }
                val tmp_ring = (getSystemService(Context.AUDIO_SERVICE) as AudioManager).ringerMode
                Log.d("030_recv_ring", "noti/dnd: $dnd, ring: $bakRingMode => $tmp_ring")
                bakRingMode = tmp_ring
            }
        }
        val filter_ringer = IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION)
        registerReceiver(recv_ringer, filter_ringer)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val notiMan = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        var newCurApp = ""
        if (event == null) return
        if (newCurApp.isEmpty()) {
            newCurApp = event.packageName?.toString() ?: return // TODO: NPE?
            if (blacklist.contains(newCurApp)) {
                Log.d("030_dnd", "ignored")
                return
            }
        }
        if (newCurApp != curApp) {
            Log.d("030_ev+app", "${eventTypeToString(event.eventType)}, prev app = $curApp, current app = $newCurApp")

            // workaround false positive with Shizuku
            if (Storage.shizukuMode && newCurApp.contains(Storage.launcher) && (event.eventType == TYPE_WINDOW_STATE_CHANGED || event.eventType == TYPE_VIEW_FOCUSED)) {
                val now = (Calendar.getInstance().timeInMillis)
                if ((now - lastEvtTime) > 500) {
                    try {
                        newCurApp = checkCurrentAppWithShizuku()
                        lastEvtTime = now
                    } catch (ex: Exception) {
                        Log.e("030-shizuku", "probably not ready?", ex)
                    }
                    if (newCurApp == curApp) return
                }
            }

            curApp = newCurApp
            appList = Storage.prefs_str.value!!

            if (appList.contains(curApp)) {
                bakNotiState = notiMan.currentInterruptionFilter
                if (bakNotiState == 1)
                    notiMan.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALARMS)
                state = 1
            } else if (state == 1) {
                state = 0
                if (bakNotiState != notiMan.currentInterruptionFilter) {
                    notiMan.setInterruptionFilter(bakNotiState)
                    am.ringerMode = bakRingMode
                }
            }
        }
    }

    fun checkCurrentAppWithShizuku(): String {
        var ret = ""
        val proc = Shizuku.newProcess(arrayOf("sh", "-c", "dumpsys activity activities | grep mFocusedApp | sed 's/.*\\ u.\\ //;s/\\/.*//'"), null, "/")
        val _output = BufferedReader(InputStreamReader(proc.inputStream))
        val fullOutput = _output.readLines()
        // workaround: somehow there might not be just one app with mFocusedApp value, so far the first one seem to be the wrong one
        if (fullOutput.size > 0) ret = fullOutput.last()
        Log.d("030_app_shizuku", ret)
        return ret
    }
}