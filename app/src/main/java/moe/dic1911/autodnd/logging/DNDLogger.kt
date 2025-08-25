package moe.dic1911.autodnd.logging

import android.app.NotificationManager
import android.util.Log

/**
 * Centralized logging system for AutoDND app
 * Provides structured logging with clear reasons for DND state changes
 */
object DNDLogger {

    private const val TAG_PREFIX = "AutoDND"

    // Log categories
    private const val TAG_DND_STATE = "$TAG_PREFIX-DND"
    private const val TAG_APP_SWITCH = "$TAG_PREFIX-App"
    private const val TAG_SCREEN = "$TAG_PREFIX-Screen"
    private const val TAG_SETUP = "$TAG_PREFIX-Setup"
    private const val TAG_STORAGE = "$TAG_PREFIX-Storage"
    private const val TAG_SHIZUKU = "$TAG_PREFIX-Shizuku"
    private const val TAG_UI = "$TAG_PREFIX-UI"
    private const val TAG_SYSTEM = "$TAG_PREFIX-System"

    // DND State Change Reasons
    enum class DNDChangeReason(val description: String) {
        APP_ENTERED_DND_LIST("Entered DND-enabled app"),
        APP_EXITED_DND_LIST("Exited DND-enabled app"),
        SCREEN_OFF_DISABLE("Screen turned off - disabling DND"),
        SCREEN_ON_RESTORE("Screen turned on - restoring previous state"),
        RINGER_MODE_BACKUP("Backing up ringer mode"),
        MANUAL_OVERRIDE("Manual DND state override detected")
    }

    // DND State logging
    fun logDNDStateChange(
        reason: DNDChangeReason,
        appPackage: String? = null,
        fromState: Int,
        toState: Int,
        additionalInfo: String? = null
    ) {
        val stateFrom = interruptionFilterToString(fromState)
        val stateTo = interruptionFilterToString(toState)
        val appInfo = appPackage?.let { " (app: $it)" } ?: ""
        val extra = additionalInfo?.let { " - $it" } ?: ""

        Log.i(TAG_DND_STATE, "DND State Change: ${reason.description}$appInfo | $stateFrom → $stateTo$extra")
    }

    fun logDNDAttempt(
        reason: DNDChangeReason,
        appPackage: String? = null,
        targetState: Int,
        success: Boolean,
        error: String? = null
    ) {
        val target = interruptionFilterToString(targetState)
        val appInfo = appPackage?.let { " (app: $it)" } ?: ""
        val result = if (success) "SUCCESS" else "FAILED"
        val errorInfo = error?.let { " - Error: $it" } ?: ""

        Log.i(TAG_DND_STATE, "DND Attempt: ${reason.description}$appInfo → $target | $result$errorInfo")
    }

    // App switching logging
    fun logAppSwitch(previousApp: String, currentApp: String, isDNDApp: Boolean) {
        val dndStatus = if (isDNDApp) "[DND-ENABLED]" else "[REGULAR]"
        Log.d(TAG_APP_SWITCH, "App Switch: $previousApp → $currentApp $dndStatus")
    }

    fun logAppIgnored(appPackage: String, reason: String) {
        Log.d(TAG_APP_SWITCH, "App Ignored: $appPackage - $reason")
    }

    // Screen state logging
    fun logScreenStateChange(isScreenOn: Boolean, dndState: Int, action: String) {
        val screenState = if (isScreenOn) "ON" else "OFF"
        val dndInfo = interruptionFilterToString(dndState)
        Log.d(TAG_SCREEN, "Screen $screenState | DND: $dndInfo | Action: $action")
    }

    // Setup and permissions logging
    fun logSetupStatus(status: Int, description: String) {
        Log.i(TAG_SETUP, "Setup Status: $status - $description")
    }

    fun logPermissionCheck(permission: String, granted: Boolean) {
        val status = if (granted) "GRANTED" else "DENIED"
        Log.d(TAG_SETUP, "Permission Check: $permission = $status")
    }

    // Storage operations logging
    fun logAppListOperation(operation: String, appPackage: String, listSize: Int) {
        Log.d(TAG_STORAGE, "App List $operation: $appPackage | List size: $listSize")
    }

    fun logPreferencesOperation(operation: String, key: String, value: String? = null) {
        val valueInfo = value?.let { " = $it" } ?: ""
        Log.d(TAG_STORAGE, "Preferences $operation: $key$valueInfo")
    }

    // Shizuku logging
    fun logShizukuOperation(operation: String, result: String? = null, error: Throwable? = null) {
        if (error != null) {
            Log.e(TAG_SHIZUKU, "Shizuku $operation FAILED: ${error.message}", error)
        } else {
            val resultInfo = result?.let { " - $it" } ?: ""
            Log.d(TAG_SHIZUKU, "Shizuku $operation$resultInfo")
        }
    }

    // UI operations logging
    fun logUIOperation(operation: String, details: String? = null) {
        val info = details?.let { " - $it" } ?: ""
        Log.d(TAG_UI, "UI $operation$info")
    }

    // System events logging
    fun logSystemEvent(event: String, details: String? = null) {
        val info = details?.let { " - $it" } ?: ""
        Log.d(TAG_SYSTEM, "System Event: $event$info")
    }

    // Error logging
    fun logError(category: String, message: String, throwable: Throwable? = null) {
        val tag = "$TAG_PREFIX-$category"
        if (throwable != null) {
            Log.e(tag, "ERROR: $message", throwable)
        } else {
            Log.e(tag, "ERROR: $message")
        }
    }

    // Utility function to convert interruption filter to readable string
    private fun interruptionFilterToString(filter: Int): String {
        return when (filter) {
            NotificationManager.INTERRUPTION_FILTER_ALL -> "ALL"
            NotificationManager.INTERRUPTION_FILTER_PRIORITY -> "PRIORITY"
            NotificationManager.INTERRUPTION_FILTER_NONE -> "NONE"
            NotificationManager.INTERRUPTION_FILTER_ALARMS -> "ALARMS"
            NotificationManager.INTERRUPTION_FILTER_UNKNOWN -> "UNKNOWN"
            else -> "CUSTOM($filter)"
        }
    }

    // Debug logging (can be disabled in production)
    fun logDebug(category: String, message: String) {
        val tag = "$TAG_PREFIX-$category"
        Log.d(tag, message)
    }

    // Verbose logging for detailed debugging
    fun logVerbose(category: String, message: String) {
        val tag = "$TAG_PREFIX-$category"
        Log.v(tag, message)
    }
}