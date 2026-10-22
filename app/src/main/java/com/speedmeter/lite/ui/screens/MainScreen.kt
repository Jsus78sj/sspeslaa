package com.speedmeter.lite.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.speedmeter.lite.data.local.AppDatabase
import com.speedmeter.lite.data.local.entity.DailyUsage
import com.speedmeter.lite.service.ServiceController
import com.speedmeter.lite.service.SpeedRepository
import com.speedmeter.lite.ui.components.UsageAccessCard
import com.speedmeter.lite.ui.components.rememberUsageAccess
import com.speedmeter.lite.ui.theme.PrimaryBlue
import com.speedmeter.lite.ui.theme.UploadGreen
import com.speedmeter.lite.utils.DateUtils
import com.speedmeter.lite.utils.FormatUtils
import com.speedmeter.lite.utils.NetworkStatsHelper
import com.speedmeter.lite.utils.SettingsManager
import com.speedmeter.lite.utils.TodayUsage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.ln

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val speed by SpeedRepository.speed.collectAsState()
    val running by SpeedRepository.running.collectAsState()
    val hasAccess by rememberUsageAccess()

    var today by remember { mutableStateOf(TodayUsage(0L, 0L)) }
    val historyFlow = remember { AppDatabase.get(context).usageDao().getLast30Days() }
    val history by historyFlow.collectAsState(initial = emptyList())

    // استهلاك اليوم الحي: يتحدث كل 5 ثوانٍ على خيط الإدخال/الإخراج وليس الرئيسي
    LaunchedEffect(hasAccess) {
        if (!hasAccess) {
            today = TodayUsage(0L, 0L)
            return@LaunchedEffect
        }
        val helper = NetworkStatsHelper(context)
        while (true) {
            today = withContext(Dispatchers.IO) { helper.getTodayUsage() } ?: TodayUsage(0L, 0L)
            delay(5000)
        }
    }

    val week = remember(history, today, hasAccess) { buildWeek(history, today, hasAccess) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Internet Speed Meter Lite",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(20.dp))

        SpeedCircleCard(speed.down, speed.up)

        Spacer(Modifier.height(16.dp))

        if (!hasAccess) {
            UsageAccessCard()
            Spacer(Modifier.height(16.dp))
        }

        val total = today.total.coerceAtLeast(1L)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            UsageCard("بيانات الجوال", FormatUtils.bytes(today.mobile), PrimaryBlue,
                today.mobile.toFloat() / total, Modifier.weight(1f))
            UsageCard("الواي فاي", FormatUtils.bytes(today.wifi), UploadGreen,
                today.wifi.toFloat() / total, Modifier.weight(1f))
        }

        Spacer(Modifier.height(16.dp))
        WeeklyChartCard(week)
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                scope.launch {
                    if (running) {
                        SettingsManager.setServiceEnabled(context, false)
                        ServiceController.stop(context)
                    } else {
                        SettingsManager.setServiceEnabled(context, true)
                        ServiceController.start(context)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
        ) {
            Text(
                text = if (running) "إيقاف الإشعار" else "تفعيل الإشعار في شريط الحالة",
                fontSize = 15.sp, color = Color.White
            )
        }
    }
}

/** آخر 7 أيام (قديم ← جديد). قيمة اليوم تؤخذ من القياس الحي لا من آخر حفظ دوري. */
private fun buildWeek(history: List<DailyUsage>, today: TodayUsage, live: Boolean): List<Pair<String, Long>> {
    val byDate = history.associate { it.date to it.totalBytes }.toMutableMap()
    if (live) byDate[DateUtils.dayKey()] = today.total
    return (6 downTo 0).map { daysAgo ->
        Pair(DateUtils.dayOfMonth(daysAgo), byDate[DateUtils.dayKey(daysAgo)] ?: 0L)
    }
}

@Composable
private fun cardColors() = CardDefaults.cardColors(
    containerColor = MaterialTheme.colorScheme.surface,
    contentColor = MaterialTheme.colorScheme.onSurface
)

@Composable
fun SpeedCircleCard(downBps: Long, upBps: Long) {
    // مقياس لوغاريتمي: من 0 إلى ~50 MB/s حتى تظهر السرعات المنخفضة والعالية معاً
    val fraction = (ln(1.0 + downBps / 1024.0) / ln(1.0 + 50.0 * 1024.0)).toFloat().coerceIn(0f, 1f)
    val sweep by animateFloatAsState(targetValue = fraction * 270f, label = "sweep")
    val track = MaterialTheme.colorScheme.surfaceVariant
    val (value, unit) = FormatUtils.speedParts(downBps)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = cardColors(),
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(
                        color = track, startAngle = 135f, sweepAngle = 270f, useCenter = false,
                        style = Stroke(width = 14f, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = PrimaryBlue, startAngle = 135f, sweepAngle = sweep, useCenter = false,
                        style = Stroke(width = 14f, cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = value, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    Text(text = unit, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "↑ ${FormatUtils.speed(upBps)}", fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SpeedLabel("التحميل", "↓", PrimaryBlue)
                SpeedLabel("الرفع", "↑", UploadGreen)
            }
        }
    }
}

@Composable
fun UsageCard(title: String, value: String, color: Color, fraction: Float, modifier: Modifier) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Card(shape = RoundedCornerShape(16.dp), colors = cardColors(), modifier = modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(text = title, fontSize = 12.sp, color = muted)
            Spacer(Modifier.height(6.dp))
            Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Box(
                Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).fillMaxHeight().background(color))
            }
            Text(text = "اليوم", fontSize = 11.sp, color = muted)
        }
    }
}

@Composable
fun WeeklyChartCard(week: List<Pair<String, Long>>) {
    val max = (week.maxOfOrNull { it.second } ?: 0L).coerceAtLeast(1L)
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Card(shape = RoundedCornerShape(16.dp), colors = cardColors(), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(text = "استهلاك آخر 7 أيام", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth().height(60.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                week.forEach { (_, bytes) ->
                    val h = (bytes.toFloat() / max).coerceIn(0.03f, 1f)
                    Box(
                        Modifier.width(22.dp).fillMaxHeight(h)
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(PrimaryBlue.copy(alpha = 0.8f))
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                week.forEach { (label, _) ->
                    Text(
                        text = label, fontSize = 11.sp, color = muted,
                        modifier = Modifier.width(22.dp), textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun SpeedLabel(title: String, icon: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(RoundedCornerShape(50)).background(color))
        Spacer(Modifier.width(6.dp))
        Text(text = "$icon $title", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
