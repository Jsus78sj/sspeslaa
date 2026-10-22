package com.speedmeter.lite

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.speedmeter.lite.service.ServiceController
import com.speedmeter.lite.ui.navigation.AppNavHost
import com.speedmeter.lite.ui.theme.SpeedMeterTheme
import com.speedmeter.lite.utils.SettingsManager
import com.speedmeter.lite.utils.ThemeManager
import com.speedmeter.lite.worker.UsageWorker
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    // الخدمة تعمل حتى لو رُفض إذن الإشعارات (فقط لن يظهر الإشعار)
    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { startServiceIfEnabled() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) requestRuntimePermissions() else startServiceIfEnabled()
        scheduleUsageWork()

        setContent {
            val themeFlow = remember { ThemeManager.getThemeFlow(this) }
            val theme by themeFlow.collectAsState(initial = ThemeManager.LIGHT)
            SpeedMeterTheme(theme = theme) {
                AppNavHost()
            }
        }
    }

    private fun requestRuntimePermissions() {
        val needed = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !granted(Manifest.permission.POST_NOTIFICATIONS)) {
            needed += Manifest.permission.POST_NOTIFICATIONS
        }
        // Android 7–9 فقط: لازم لقراءة معرّف الشريحة عند قياس بيانات الجوال
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && !granted(Manifest.permission.READ_PHONE_STATE)) {
            needed += Manifest.permission.READ_PHONE_STATE
        }
        if (needed.isEmpty()) startServiceIfEnabled() else requestPermissions.launch(needed.toTypedArray())
    }

    private fun granted(permission: String) =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    private fun startServiceIfEnabled() {
        lifecycleScope.launch {
            if (SettingsManager.isServiceEnabled(this@MainActivity)) ServiceController.start(this@MainActivity)
        }
    }

    private fun scheduleUsageWork() {
        val wm = WorkManager.getInstance(this)
        wm.enqueueUniquePeriodicWork(
            "usage_work", ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<UsageWorker>(1, TimeUnit.HOURS).build()
        )
        // تحديث فوري لسجل اليوم عند فتح التطبيق
        wm.enqueueUniqueWork("usage_now", ExistingWorkPolicy.REPLACE, OneTimeWorkRequestBuilder<UsageWorker>().build())
    }
}
