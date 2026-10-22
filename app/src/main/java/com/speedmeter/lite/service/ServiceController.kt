package com.speedmeter.lite.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

object ServiceController {
    /** يبدأ الخدمة؛ يعيد false إذا منع النظام ذلك (مثلاً قيود الخلفية في Android 12+). */
    fun start(context: Context): Boolean = try {
        ContextCompat.startForegroundService(context, Intent(context, SpeedService::class.java))
        true
    } catch (e: IllegalStateException) {
        false
    } catch (e: SecurityException) {
        false
    }

    fun stop(context: Context) {
        context.stopService(Intent(context, SpeedService::class.java))
    }
}
