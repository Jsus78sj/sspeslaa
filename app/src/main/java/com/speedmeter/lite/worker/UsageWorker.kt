package com.speedmeter.lite.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.speedmeter.lite.data.local.AppDatabase
import com.speedmeter.lite.data.local.entity.DailyUsage
import com.speedmeter.lite.utils.DateUtils
import com.speedmeter.lite.utils.NetworkStatsHelper
import com.speedmeter.lite.utils.UsageAccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UsageWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val ctx = applicationContext
        // بدون الصلاحية لا نكتب شيئاً؛ حتى لا تُستبدل بيانات اليوم السابقة بأصفار.
        if (!UsageAccess.isGranted(ctx)) return Result.success()

        val usage = withContext(Dispatchers.IO) { NetworkStatsHelper(ctx).getTodayUsage() }
            ?: return Result.success()

        val dao = AppDatabase.get(ctx).usageDao()
        dao.insert(DailyUsage(DateUtils.dayKey(), usage.mobile, usage.wifi, usage.total))
        dao.deleteOlderThan(DateUtils.dayKey(daysAgo = 30))
        return Result.success()
    }
}
