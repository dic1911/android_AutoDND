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
import moe.dic1911.autodnd.logging.DNDLogger
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
    private var dndByUs = false
    private var stateBeforeScreenOff: Int? = NotificationManager.INTERRUPTION_FILTER_UNKNOWN

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
                    DNDLogger.logSystemEvent("Ringer mode change ignored", "DND app active or DND already on")
                    return
                }
                val tmp_ring = (getSystemService(Context.AUDIO_SERVICE) as AudioManager).ringerMode
                DNDLogger.logDNDStateChange(
                    DNDLogger.DNDChangeReason.RINGER_MODE_BACKUP,
                    fromState = bakRingMode,
                    toState = tmp_ring,
                    additionalInfo = "DND filter: $dnd"
                )
                bakRingMode = tmp_ring
            }
        }
        val filter_ringer = IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION)
        registerReceiver(recv_ringer, filter_ringer)

        try {
            val filterScreenOnOff = IntentFilter(Intent.ACTION_SCREEN_ON)
            filterScreenOnOff.addAction(Intent.ACTION_SCREEN_OFF)
            val screenReceiver = ScreenReceiver()
            registerReceiver(screenReceiver, filterScreenOnOff)
            screenReceiver.addCallback(1) { screenOn ->
                val notiMan = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val currentDNDState = notiMan.currentInterruptionFilter

                if (screenOn && stateBeforeScreenOff != NotificationManager.INTERRUPTION_FILTER_UNKNOWN) {
                    stateBeforeScreenOff?.let { savedState ->
                        DNDLogger.logDNDStateChange(
                            DNDLogger.DNDChangeReason.SCREEN_ON_RESTORE,
                            fromState = currentDNDState,
                            toState = savedState,
                            additionalInfo = "Restoring state from before screen off"
                        )
                        notiMan.setInterruptionFilter(savedState)
                    }
                }

                if (!screenOn) {
                    // backup current state after off + turn off dnd if turned on by us
                    stateBeforeScreenOff = currentDNDState
                    DNDLogger.logScreenStateChange(false, currentDNDState, "Saved current DND state")

                    if (state == 1) {
                        DNDLogger.logDNDStateChange(
                            DNDLogger.DNDChangeReason.SCREEN_OFF_DISABLE,
                            appPackage = curApp,
                            fromState = currentDNDState,
                            toState = NotificationManager.INTERRUPTION_FILTER_ALL,
                            additionalInfo = "Auto-disabling DND due to screen off"
                        )
                        notiMan.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                    }
                }
            }
        } catch (ex: Exception) {
            DNDLogger.logError("Screen", "Failed to register receiver for screen events", ex)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val notiMan = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        var newCurApp = ""
        if (event == null) return
        if (newCurApp.isEmpty()) {
            newCurApp = event.packageName?.toString() ?: return // TODO: NPE?
            if (blacklist.contains(newCurApp)) {
                DNDLogger.logAppIgnored(newCurApp, "App in blacklist")
                return
            }
        }
        if (newCurApp != curApp) {
            DNDLogger.logAppSwitch(curApp, newCurApp, appList.contains(newCurApp))
            DNDLogger.logDebug("Event", "${eventTypeToString(event.eventType)} - App switch detected")

            // workaround false positive with Shizuku
            if (Storage.shizukuMode && newCurApp.contains(Storage.launcher) && (event.eventType == TYPE_WINDOW_STATE_CHANGED || event.eventType == TYPE_VIEW_FOCUSED)) {
                val now = (Calendar.getInstance().timeInMillis)
                if ((now - lastEvtTime) > 500) {
                    try {
                        newCurApp = checkCurrentAppWithShizuku()
                        lastEvtTime = now
                    } catch (ex: Exception) {
                        DNDLogger.logShizukuOperation("Current app check", error = ex)
                    }
                    if (newCurApp == curApp) return
                }
            }

            curApp = newCurApp
            appList = Storage.prefs_str.value!!

            if (appList.contains(curApp)) {
                // Entering DND-enabled app
                val currentState = notiMan.currentInterruptionFilter
                bakNotiState = currentState

                if (bakNotiState == NotificationManager.INTERRUPTION_FILTER_ALL) {
                    DNDLogger.logDNDStateChange(
                        DNDLogger.DNDChangeReason.APP_ENTERED_DND_LIST,
                        appPackage = curApp,
                        fromState = currentState,
                        toState = NotificationManager.INTERRUPTION_FILTER_ALARMS,
                        additionalInfo = "Enabling DND for app"
                    )
                    notiMan.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALARMS)
                } else {
                    DNDLogger.logDNDAttempt(
                        DNDLogger.DNDChangeReason.APP_ENTERED_DND_LIST,
                        appPackage = curApp,
                        targetState = NotificationManager.INTERRUPTION_FILTER_ALARMS,
                        success = false,
                        error = "DND already active (state: $currentState)"
                    )
                }
                state = 1
            } else if (state == 1) {
                // Exiting DND-enabled app
                val currentState = notiMan.currentInterruptionFilter
                state = 0

                if (bakNotiState != currentState) {
                    DNDLogger.logDNDStateChange(
                        DNDLogger.DNDChangeReason.APP_EXITED_DND_LIST,
                        appPackage = curApp,
                        fromState = currentState,
                        toState = bakNotiState,
                        additionalInfo = "Restoring previous DND state and ringer mode"
                    )
                    notiMan.setInterruptionFilter(bakNotiState)
                    am.ringerMode = bakRingMode
                } else {
                    DNDLogger.logDebug("DND", "No DND state change needed - states match (current: $currentState, backup: $bakNotiState)")
                }
            }
        }
    }

    fun checkCurrentAppWithShizuku(): String {
        var ret = ""
        // updated@2025: filter mFocusedApp=null
        val proc = Shizuku.newProcess(arrayOf("sh", "-c",
            "dumpsys activity activities | grep mFocusedApp | sed 's/.*\\ u.\\ //;s/\\/.*//' | grep -E -v 'null$'"
        ), null, "/")
        val _output = BufferedReader(InputStreamReader(proc.inputStream))
        val fullOutput = _output.readLines()
        // workaround: somehow there might not be just one app with mFocusedApp value, so far the first one seem to be the wrong one
        if (fullOutput.size > 0) ret = fullOutput.last()
        DNDLogger.logShizukuOperation("Current app detection", "Detected app: $ret (from ${fullOutput.size} results)")
        return ret
    }
}