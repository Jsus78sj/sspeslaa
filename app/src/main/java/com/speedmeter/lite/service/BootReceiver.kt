package com.speedmeter.lite.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.speedmeter.lite.utils.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** يعيد تشغيل الخدمة بعد إقلاع الجهاز إذا كان المستخدم لم يوقفها. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val app = context.applicationContext
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                if (SettingsManager.isServiceEnabled(app)) ServiceController.start(app)
            } finally {
                pending.finish()
            }
        }
    }
}
