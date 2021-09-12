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
import android.view.accessibility.AccessibilityEvent.*
import androidx.lifecycle.Observer
import moe.dic1911.autodnd.data.Storage


class DNDAccessibilityService : AccessibilityService() {
    private var appList: ArrayList<String> = ArrayList()
    private val blacklist: ArrayList<String> = ArrayList()
    private val info = AccessibilityServiceInfo()
    private var curApp = ""
    private var state = 0
    private var bakNotiState = NotificationManager.INTERRUPTION_FILTER_ALL
    private var bakRingMode = AudioManager.RINGER_MODE_VIBRATE

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

        /*val recv_noti: BroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (state == 1) return
                bakNotiState = (getSystemService(Context.NOTIFICATION_SERVICE)
                        as NotificationManager).currentInterruptionFilter
                Log.d("030_recv_noti", "noti/dnd: $bakNotiState, ring: $bakRingMode")
            }
        }
        val filter_noti = IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION)
        //registerReceiver(recv_noti, filter_noti)*/

        val recv_ringer: BroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val dnd = (getSystemService(Context.NOTIFICATION_SERVICE)
                        as NotificationManager).currentInterruptionFilter
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
                //Log.d("030_ev+app", "${eventTypeToString(event.eventType)}, current app = $curApp")
                appList = Storage.prefs_str.value!!

                if (appList.contains(curApp)) {

                    bakNotiState = notiMan.currentInterruptionFilter
                    //bakRingMode = am.ringerMode // use value we had obtained earlier instead

                    //Log.d("030_preautodnd", "noti/dnd: $bakNotiState, ring: $bakRingMode")
                    if (bakNotiState == 1)
                        notiMan.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALARMS)
                    state = 1
                } else if (state == 1) {
                    state = 0
                    if (bakNotiState != notiMan.currentInterruptionFilter) {
                        //notiMan.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                        notiMan.setInterruptionFilter(bakNotiState)
                        am.ringerMode = bakRingMode //AudioManager.RINGER_MODE_VIBRATE
                    }
                }
            }
            /*Log.d("030", AccessibilityEvent.eventTypeToString(event.eventType))
            if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
                Log.d("030", event.packageName.toString())
            }*/
        }/* else {
            Log.d("030", "Null event!")
        }*/
    }

}