package moe.dic1911.autodnd

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.util.Consumer

class ScreenReceiver : BroadcastReceiver() {
    val callbacks = HashMap<Int, Consumer<Boolean>>()

    override fun onReceive(context: Context?, intent: Intent) {
        if (intent.action == Intent.ACTION_SCREEN_OFF) {
            isScreenOn = false
        } else if (intent.action == Intent.ACTION_SCREEN_ON) {
            isScreenOn = true
        }

        for (e in callbacks) {
            try {
                e.value.accept(isScreenOn)
            } catch (ex: Exception) {
                Log.e("030-scr-callback", "on exec ${e.key}", ex)
            }
        }
    }

    fun addCallback(id: Int, callback: Consumer<Boolean>) {
        callbacks[id] = callback
    }

    companion object {
        var isScreenOn: Boolean = false
    }
}