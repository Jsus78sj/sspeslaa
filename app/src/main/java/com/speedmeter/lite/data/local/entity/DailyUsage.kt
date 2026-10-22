package com.speedmeter.lite.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_usage")
data class DailyUsage(
    @PrimaryKey val date: String,
    val mobileBytes: Long = 0,
    val wifiBytes: Long = 0,
    val totalBytes: Long = 0
)
