package com.speedmeter.lite.utils

import android.Manifest
import android.annotation.SuppressLint
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat

data class TodayUsage(val mobile: Long, val wifi: Long) {
    val total: Long get() = mobile + wifi
}

/**
 * معرّف المشترك لشبكة الجوال.
 * - Android 10+: يُمرَّر null (قراءة subscriberId محظورة على التطبيقات العادية وتُرجع بيانات كل الشرائح).
 * - Android 7–9: يحتاج READ_PHONE_STATE (يُطلب وقت التشغيل على هذه الإصدارات فقط).
 */
@SuppressLint("HardwareIds", "MissingPermission")
internal fun mobileSubscriberId(context: Context): String? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) return null
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) !=
        PackageManager.PERMISSION_GRANTED
    ) return null
    return try {
        (context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager).subscriberId
    } catch (e: SecurityException) {
        null
    }
}

class NetworkStatsHelper(private val context: Context) {

    /**
     * استهلاك اليوم (من منتصف الليل المحلي). يعيد null إذا لم تُمنح صلاحية الوصول لبيانات الاستخدام،
     * لتمييز «لا صلاحية» عن «استهلاك صفر».
     */
    @Suppress("DEPRECATION")
    fun getTodayUsage(): TodayUsage? {
        val nsm = context.getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager
        val start = DateUtils.startOfToday()
        val now = System.currentTimeMillis()
        return try {
            val wifi = deviceBytes(nsm, ConnectivityManager.TYPE_WIFI, "", start, now)
            val mobile = deviceBytes(nsm, ConnectivityManager.TYPE_MOBILE, mobileSubscriberId(context), start, now)
            TodayUsage(mobile = mobile, wifi = wifi)
        } catch (e: SecurityException) {
            null
        }
    }

    private fun deviceBytes(
        nsm: NetworkStatsManager, type: Int, subscriberId: String?, start: Long, end: Long
    ): Long = try {
        val b = nsm.querySummaryForDevice(type, subscriberId, start, end)
        b.rxBytes + b.txBytes
    } catch (e: SecurityException) {
        throw e
    } catch (e: Exception) {
        0L // شبكة غير متاحة على الجهاز (مثلاً جهاز بلا شريحة)
    }
}
