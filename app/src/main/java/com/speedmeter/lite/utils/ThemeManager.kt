package com.speedmeter.lite.utils

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore by preferencesDataStore("settings")

/** تدفق التفضيلات مع تجاهل أخطاء القراءة (ملف تالف/IO) بدل إسقاط التطبيق. */
internal fun Context.prefsFlow(): Flow<Preferences> =
    dataStore.data.catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }

object ThemeManager {
    const val LIGHT = "فاتح"
    const val DARK = "داكن"
    const val NAVY = "أزرق ليلي"

    private val THEME_KEY = stringPreferencesKey("theme")
    val themes = listOf(LIGHT, DARK, NAVY)

    fun getThemeFlow(context: Context): Flow<String> =
        context.prefsFlow().map { it[THEME_KEY] ?: LIGHT }

    suspend fun setTheme(context: Context, theme: String) {
        context.dataStore.edit { it[THEME_KEY] = theme }
    }
}
