package com.speedmeter.lite.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.speedmeter.lite.utils.SettingsManager
import com.speedmeter.lite.utils.ThemeManager
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val themeFlow = remember { ThemeManager.getThemeFlow(context) }
    val separateFlow = remember { SettingsManager.separateFlow(context) }
    val smartFlow = remember { SettingsManager.smartFlow(context) }
    val selected by themeFlow.collectAsState(initial = ThemeManager.LIGHT)
    val separateSpeed by separateFlow.collectAsState(initial = false)
    val smartNotification by smartFlow.collectAsState(initial = true)

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("الإعدادات", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))

        Text("الثيم - مجاني للجميع")
        ThemeManager.themes.forEach { theme ->
            Row(
                Modifier.fillMaxWidth().selectable(
                    selected = selected == theme,
                    onClick = { scope.launch { ThemeManager.setTheme(context, theme) } }
                ).padding(12.dp)
            ) {
                RadioButton(selected = selected == theme, onClick = null)
                Spacer(Modifier.width(8.dp))
                Text(theme)
            }
        }
        Divider(Modifier.padding(vertical = 12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("فصل سرعة الرفع والتحميل في اشعارين", Modifier.weight(1f))
            Switch(
                checked = separateSpeed,
                onCheckedChange = { v -> scope.launch { SettingsManager.setSeparate(context, v) } }
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("الاشعار الذكي - يظهر فقط عند الاتصال", Modifier.weight(1f))
            Switch(
                checked = smartNotification,
                onCheckedChange = { v -> scope.launch { SettingsManager.setSmart(context, v) } }
            )
        }
        Text(
            "كل المميزات مفتوحة مجانا في هذه النسخة",
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}
