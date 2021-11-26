package moe.dic1911.autodnd

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Application
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.util.Log
import android.util.TimeUtils
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityEvent.*
import moe.dic1911.autodnd.data.Storage


class DNDAccessibilityService : AccessibilityService() {
    private var appList: ArrayList<String> = ArrayList()
    private val blacklist: ArrayList<String> = ArrayList()
    private val info = AccessibilityServiceInfo()
    private var curApp = ""
    private var state = 0
    private var bakNotiState = NotificationManager.INTERRUPTION_FILTER_ALL
    private var bakRingMode = AudioManager.RINGER_MODE_VIBRATE
    private var launcherTimestamp: Long = 0

    override fun onInterrupt() {}

    override fun onServiceConnected() {
        super.onServiceConnected()
        info.eventTypes = TYPES_ALL_MASK
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
        if (event != null) {
            if (event.eventType != TYPE_WINDOWS_CHANGED &&
                event.eventType != TYPE_WINDOW_CONTENT_CHANGED &&
                event.eventType != TYPE_WINDOW_STATE_CHANGED) return

            val newCurApp = event.packageName?.toString() ?: return // TODO: NPE?
            if (newCurApp != curApp && !blacklist.contains(newCurApp)) {
                curApp = newCurApp
                Log.d("030_ev+app", "${eventTypeToString(event.eventType)}, current app = $curApp")
                appList = Storage.prefs_str.value!!

                if (appList.contains(curApp)) {
                    bakNotiState = notiMan.currentInterruptionFilter
                    if (bakNotiState == 1)
                        notiMan.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALARMS)
                    state = 1
                } else if (state == 1) {
                    // workaround against invalid(?) events triggered by gesture
                    if (false && curApp.contains("launcher")) {
                        val popup = Intent(this, PopupActivity()::class.java)
                        popup.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(popup)
                    }
                    state = 0
                    if (bakNotiState != notiMan.currentInterruptionFilter) {
                        notiMan.setInterruptionFilter(bakNotiState)
                        am.ringerMode = bakRingMode
                    }
                }
            }
        }
    }
}