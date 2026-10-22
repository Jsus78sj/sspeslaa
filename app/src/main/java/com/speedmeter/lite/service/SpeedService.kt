package com.speedmeter.lite.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.speedmeter.lite.MainActivity
import com.speedmeter.lite.R
import com.speedmeter.lite.utils.FormatUtils
import com.speedmeter.lite.utils.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

class SpeedService : Service() {

    private companion object {
        const val NOTIF_DOWN_ID = 1
        const val NOTIF_UP_ID = 2
        const val CHANNEL_SPEED = "speed_channel_id"
        const val CHANNEL_UPLOAD = "speed_upload_id"
        // قناة بأهمية MIN: لا تظهر أيقونتها في شريط الحالة. نستخدمها في «الإشعار الذكي» عند انقطاع الاتصال
        // بدل stopForeground/startForeground الذي قد يرمي استثناء في Android 12+.
        const val CHANNEL_IDLE = "speed_idle_id"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var loopJob: Job? = null

    private lateinit var nm: NotificationManager
    private lateinit var cm: ConnectivityManager

    private var sessionStart = 0L
    private var lastRx = 0L
    private var lastTx = 0L
    private var lastTs = 0L
    private var upShown = false

    @Volatile private var separateMode = false
    @Volatile private var smartMode = true
    @Volatile private var connected = true
    private var callbackRegistered = false

    private val contentIntent: PendingIntent by lazy {
        PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private val netCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) { connected = true }
        override fun onLost(network: Network) { connected = false }
        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
            connected = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }
    }

    override fun onCreate() {
        super.onCreate()
        nm = getSystemService(NotificationManager::class.java)
        cm = getSystemService(ConnectivityManager::class.java)
        createChannels()
        sessionStart = SystemClock.elapsedRealtime()
        connected = isConnected()
        try {
            cm.registerDefaultNetworkCallback(netCallback)
            callbackRegistered = true
        } catch (e: Exception) {
            callbackRegistered = false
        }
        // الإعدادات تُقرأ من DataStore وتتحدث فوراً عند تغييرها من شاشة الإعدادات
        scope.launch { SettingsManager.separateFlow(this@SpeedService).collect { separateMode = it } }
        scope.launch { SettingsManager.smartFlow(this@SpeedService).collect { smartMode = it } }
        SpeedRepository.setRunning(true)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            ServiceCompat.startForeground(
                this, NOTIF_DOWN_ID,
                buildNotification(CHANNEL_SPEED, "↓ 0 B/s  ↑ 0 B/s", "جاري القياس..."),
                foregroundType()
            )
        } catch (e: Exception) {
            // مثلاً ForegroundServiceStartNotAllowedException عند إعادة التشغيل من الخلفية
            stopSelf()
            return START_NOT_STICKY
        }
        if (loopJob?.isActive != true) loopJob = scope.launch { measureLoop() }
        return START_STICKY
    }

    private fun foregroundType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0

    private fun isConnected(): Boolean {
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private suspend fun measureLoop() {
        lastRx = TrafficStats.getTotalRxBytes()
        lastTx = TrafficStats.getTotalTxBytes()
        lastTs = SystemClock.elapsedRealtime()

        if (lastRx == TrafficStats.UNSUPPORTED.toLong() || lastTx == TrafficStats.UNSUPPORTED.toLong()) {
            nm.notify(NOTIF_DOWN_ID, buildNotification(CHANNEL_SPEED, "غير مدعوم", "هذا الجهاز لا يوفّر إحصاءات الشبكة"))
            return
        }

        var idleShown = false
        delay(1000) // أول قياس بعد ثانية كاملة لتجنب قفزة وهمية

        while (true) {
            val now = SystemClock.elapsedRealtime()
            val rx = TrafficStats.getTotalRxBytes()
            val tx = TrafficStats.getTotalTxBytes()

            if (smartMode && !connected) {
                if (!idleShown) {
                    nm.cancel(NOTIF_UP_ID); upShown = false
                    nm.notify(NOTIF_DOWN_ID, buildNotification(CHANNEL_IDLE, "غير متصل", "بانتظار الاتصال بالإنترنت"))
                    SpeedRepository.update(0, 0)
                    idleShown = true
                }
                lastRx = rx; lastTx = tx; lastTs = now
                delay(2000)
                continue
            }
            idleShown = false

            // السرعة = الفرق في البايتات ÷ الزمن الفعلي المنقضي (لا نفترض ثانية بالضبط)
            val dt = (now - lastTs).coerceAtLeast(1L)
            val down = (rx - lastRx).coerceAtLeast(0L) * 1000L / dt
            val up = (tx - lastTx).coerceAtLeast(0L) * 1000L / dt
            lastRx = rx; lastTx = tx; lastTs = now
            SpeedRepository.update(down, up)

            val s = (now - sessionStart) / 1000
            val duration = String.format(Locale.US, "%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60)

            if (separateMode) {
                nm.notify(NOTIF_DOWN_ID, buildNotification(CHANNEL_SPEED, "↓ ${FormatUtils.speed(down)}", "تحميل - $duration"))
                nm.notify(NOTIF_UP_ID, buildNotification(CHANNEL_UPLOAD, "↑ ${FormatUtils.speed(up)}", "رفع - $duration"))
                upShown = true
            } else {
                if (upShown) { nm.cancel(NOTIF_UP_ID); upShown = false }
                nm.notify(
                    NOTIF_DOWN_ID,
                    buildNotification(
                        CHANNEL_SPEED,
                        "↓ ${FormatUtils.speed(down)}  ↑ ${FormatUtils.speed(up)}",
                        "المدة: $duration | اضغط للتفاصيل"
                    )
                )
            }
            delay(1000)
        }
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        nm.createNotificationChannel(NotificationChannel(CHANNEL_SPEED, "Speed Download", NotificationManager.IMPORTANCE_LOW))
        nm.createNotificationChannel(NotificationChannel(CHANNEL_UPLOAD, "Speed Upload", NotificationManager.IMPORTANCE_LOW))
        nm.createNotificationChannel(NotificationChannel(CHANNEL_IDLE, "Idle (no connection)", NotificationManager.IMPORTANCE_MIN))
    }

    private fun buildNotification(channel: String, title: String, sub: String): Notification =
        NotificationCompat.Builder(this, channel)
            .setContentTitle(title)
            .setContentText(sub)
            .setSmallIcon(R.drawable.ic_speed)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setPriority(if (channel == CHANNEL_IDLE) NotificationCompat.PRIORITY_MIN else NotificationCompat.PRIORITY_LOW)
            .setContentIntent(contentIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$title\n$sub"))
            .build()

    override fun onDestroy() {
        if (callbackRegistered) {
            try { cm.unregisterNetworkCallback(netCallback) } catch (e: Exception) { /* مُلغى مسبقاً */ }
        }
        scope.cancel()
        nm.cancel(NOTIF_UP_ID)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        SpeedRepository.setRunning(false)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
