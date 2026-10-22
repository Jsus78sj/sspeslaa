package com.speedmeter.lite.utils

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Process

data class AppUsageInfo(val appName: String, val packageName: String, val totalBytes: Long)

class AppUsageHelper(private val context: Context) {

    /** أعلى التطبيقات استهلاكاً اليوم (واي فاي + جوال). دالة حاجبة: استدعِها من خيط خلفي. */
    @Suppress("DEPRECATION")
    fun getAppUsageToday(limit: Int = 10): List<AppUsageInfo> {
        val nsm = context.getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager
        val pm = context.packageManager
        val start = DateUtils.startOfToday()
        val end = System.currentTimeMillis()

        val perUid = HashMap<Int, Long>()
        try {
            collect(nsm, ConnectivityManager.TYPE_WIFI, "", start, end, perUid)
            collect(nsm, ConnectivityManager.TYPE_MOBILE, mobileSubscriberId(context), start, end, perUid)
        } catch (e: SecurityException) {
            return emptyList() // لا توجد صلاحية Usage Access
        }

        val result = ArrayList<AppUsageInfo>()
        for ((uid, total) in perUid) {
            if (total <= 0 || uid < Process.FIRST_APPLICATION_UID) continue
            val pkg = pm.getPackagesForUid(uid)?.firstOrNull() ?: continue
            val info = try {
                pm.getApplicationInfo(pkg, 0)
            } catch (e: PackageManager.NameNotFoundException) {
                continue
            }
            // نستبعد تطبيقات النظام الأصلية فقط؛ تطبيقات النظام المحدَّثة (Chrome/YouTube...) تُحتسب.
            val isPureSystem = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0 &&
                (info.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0
            if (isPureSystem) continue
            result.add(AppUsageInfo(pm.getApplicationLabel(info).toString(), pkg, total))
        }
        return result.sortedByDescending { it.totalBytes }.take(limit)
    }

    private fun collect(
        nsm: NetworkStatsManager, type: Int, subscriberId: String?,
        start: Long, end: Long, out: MutableMap<Int, Long>
    ) {
        val stats = try {
            nsm.querySummary(type, subscriberId, start, end)
        } catch (e: SecurityException) {
            throw e
        } catch (e: Exception) {
            return
        }
        try {
            val bucket = NetworkStats.Bucket()
            while (stats.hasNextBucket()) {
                stats.getNextBucket(bucket)
                out[bucket.uid] = (out[bucket.uid] ?: 0L) + bucket.rxBytes + bucket.txBytes
            }
        } finally {
            stats.close()
        }
    }
}
