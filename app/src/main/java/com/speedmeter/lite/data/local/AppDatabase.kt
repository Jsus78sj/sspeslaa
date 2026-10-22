package com.speedmeter.lite.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.speedmeter.lite.data.local.dao.UsageDao
import com.speedmeter.lite.data.local.entity.DailyUsage

// عند تغيير المخطط مستقبلاً: ارفع version وأضف Migration صريحاً (لا حذف تلقائي للسجل).
@Database(entities = [DailyUsage::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun usageDao(): UsageDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext, AppDatabase::class.java, "speed_meter.db"
                ).build().also { INSTANCE = it }
            }
    }
}
