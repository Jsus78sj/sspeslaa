package com.speedmeter.lite.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.speedmeter.lite.data.local.entity.DailyUsage
import com.speedmeter.lite.ui.theme.PrimaryBlue
import com.speedmeter.lite.utils.FormatUtils

/** list مرتبة تنازلياً بالتاريخ (الأحدث أولاً) كما يعيدها UsageDao. */
@Composable
fun HistoryScreen(list: List<DailyUsage>) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    // أحدث 7 أيام ثم نعكسها لتظهر من الأقدم إلى الأحدث
    val recent = list.take(7).reversed()
    val max = (recent.maxOfOrNull { it.totalBytes } ?: 0L).coerceAtLeast(1L)

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text("سجل 30 يوم", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth().height(80.dp),
                verticalAlignment = androidx.compose.ui.Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                recent.forEach {
                    val h = (it.totalBytes.toFloat() / max).coerceIn(0.03f, 1f)
                    Box(Modifier.width(22.dp).fillMaxHeight(h).clip(RoundedCornerShape(6.dp)).background(PrimaryBlue))
                }
            }
            Spacer(Modifier.height(16.dp))
            if (list.isEmpty()) {
                Text("لا توجد بيانات بعد. تأكد من منح صلاحية «الوصول إلى بيانات الاستخدام» وسيبدأ السجل بالامتلاء.", color = muted)
            }
        }
        items(list) { item ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Row(Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(item.date, fontSize = 13.sp, color = muted)
                        Text(
                            "جوال ${FormatUtils.bytes(item.mobileBytes)} • واي فاي ${FormatUtils.bytes(item.wifiBytes)}",
                            fontSize = 11.sp, color = muted
                        )
                    }
                    Text(FormatUtils.bytes(item.totalBytes), fontSize = 14.sp)
                }
            }
        }
    }
}
