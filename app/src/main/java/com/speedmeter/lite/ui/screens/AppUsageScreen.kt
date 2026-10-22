package com.speedmeter.lite.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.speedmeter.lite.ui.components.UsageAccessCard
import com.speedmeter.lite.ui.components.rememberUsageAccess
import com.speedmeter.lite.utils.AppUsageHelper
import com.speedmeter.lite.utils.AppUsageInfo
import com.speedmeter.lite.utils.FormatUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun AppUsageScreen() {
    val context = LocalContext.current
    val hasAccess by rememberUsageAccess()
    var list by remember { mutableStateOf(emptyList<AppUsageInfo>()) }
    var loaded by remember { mutableStateOf(false) }

    // الاستعلام حاجب فيُنفَّذ على IO، ويُحدَّث كل 30 ثانية
    LaunchedEffect(hasAccess) {
        if (!hasAccess) {
            list = emptyList(); loaded = false
            return@LaunchedEffect
        }
        val helper = AppUsageHelper(context)
        while (true) {
            list = withContext(Dispatchers.IO) { helper.getAppUsageToday() }
            loaded = true
            delay(30_000)
        }
    }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text("استهلاك التطبيقات اليوم - مجاني", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
        }
        if (!hasAccess) {
            item { UsageAccessCard() }
        } else if (loaded && list.isEmpty()) {
            item { Text("لا يوجد استهلاك للتطبيقات حتى الآن اليوم") }
        }
        items(list) {
            Card(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Row(Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(it.appName)
                    Text(FormatUtils.bytes(it.totalBytes))
                }
            }
        }
    }
}
