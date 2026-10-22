package com.speedmeter.lite.utils

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** إعدادات التطبيق المخزنة في DataStore، وتقرؤها الخدمة مباشرة. */
object SettingsManager {
    private val SEPARATE = booleanPreferencesKey("separate_notifications")
    private val SMART = booleanPreferencesKey("smart_notification")
    private val SERVICE = booleanPreferencesKey("service_enabled")
    private val ICON_MODE = intPreferencesKey("status_icon_mode")

    // ما الذي يظهر كرقم في أيقونة شريط الحالة (عند عدم فصل الإشعارين)
    const val ICON_DOWN = 0
    const val ICON_UP = 1
    const val ICON_TOTAL = 2

    fun separateFlow(c: Context): Flow<Boolean> = c.prefsFlow().map { it[SEPARATE] ?: false }
    fun smartFlow(c: Context): Flow<Boolean> = c.prefsFlow().map { it[SMART] ?: true }
    fun serviceEnabledFlow(c: Context): Flow<Boolean> = c.prefsFlow().map { it[SERVICE] ?: true }
    fun iconModeFlow(c: Context): Flow<Int> = c.prefsFlow().map { it[ICON_MODE] ?: ICON_DOWN }

    suspend fun setSeparate(c: Context, v: Boolean) { c.dataStore.edit { it[SEPARATE] = v } }
    suspend fun setSmart(c: Context, v: Boolean) { c.dataStore.edit { it[SMART] = v } }
    suspend fun setServiceEnabled(c: Context, v: Boolean) { c.dataStore.edit { it[SERVICE] = v } }
    suspend fun setIconMode(c: Context, v: Int) { c.dataStore.edit { it[ICON_MODE] = v } }
    suspend fun isServiceEnabled(c: Context): Boolean = serviceEnabledFlow(c).first()
}
