package com.speedmeter.lite.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.speedmeter.lite.data.local.AppDatabase
import com.speedmeter.lite.ui.screens.AppUsageScreen
import com.speedmeter.lite.ui.screens.HistoryScreen
import com.speedmeter.lite.ui.screens.MainScreen
import com.speedmeter.lite.ui.screens.SettingsScreen

data class BottomItem(val route: String, val label: String, val icon: ImageVector)

val bottomItems = listOf(
    BottomItem("main", "الرئيسية", Icons.Filled.Speed),
    BottomItem("history", "السجل", Icons.Filled.History),
    BottomItem("apps", "التطبيقات", Icons.Filled.Apps),
    BottomItem("settings", "الإعدادات", Icons.Filled.Settings)
)

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomItems.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo("main") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "main",
            modifier = Modifier.padding(padding)
        ) {
            composable("main") { MainScreen() }
            composable("history") { HistoryRoute() }
            composable("apps") { AppUsageScreen() }
            composable("settings") { SettingsScreen() }
        }
    }
}

@Composable
fun HistoryRoute() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val flow = remember { AppDatabase.get(context).usageDao().getLast30Days() }
    val list by flow.collectAsState(initial = emptyList())
    HistoryScreen(list = list)
}
