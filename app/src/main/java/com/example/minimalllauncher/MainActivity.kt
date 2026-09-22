package com.example.minimalllauncher

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

data class LaunchableApp(
val label: String,
val packageName: String
)

class MainActivity : ComponentActivity() {

private val appList = mutableStateOf<List<LaunchableApp>>(emptyList())
private val usageAccessAvailable = mutableStateOf(false)
private val showUsageDialog = mutableStateOf(false)

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    WindowCompat.getInsetsController(window, window.decorView).hide(
        WindowInsetsCompat.Type.statusBars()
    )

    WindowCompat.setDecorFitsSystemWindows(window, false)
    window.statusBarColor = android.graphics.Color.BLACK
    window.navigationBarColor = android.graphics.Color.BLACK

    loadApps()

    setContent {
        MinimalLauncher(
            apps = appList.value,
            usageAccessAvailable = usageAccessAvailable.value,
            showUsageDialog = showUsageDialog.value,
            onLaunch = { packageName ->
                launchApp(packageName)
            },
            onOpenUsageSettings = {
                showUsageDialog.value = false
                openUsageAccessSettings()
            },
            onDismissUsageDialog = {
                showUsageDialog.value = false
            }
        )
    }
}

override fun onResume() {
    super.onResume()

    WindowCompat.getInsetsController(window, window.decorView).hide(
        WindowInsetsCompat.Type.statusBars()
    )

    loadApps()
}

private fun loadApps() {
    val pm = packageManager

    val intent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
    }

    val apps = pm.queryIntentActivities(
        intent,
        PackageManager.MATCH_ALL
    )
        .map {
            LaunchableApp(
                label = it.loadLabel(pm).toString(),
                packageName = it.activityInfo.packageName
            )
        }
        .distinctBy { it.packageName }
        .filter { it.packageName != packageName }
        .sortedWith(
            compareBy(String.CASE_INSENSITIVE_ORDER) { it.label }
        )

    val hasAccess = hasUsageAccess()
    usageAccessAvailable.value = hasAccess

    if (hasAccess) {
        val mostUsedApps = getMostUsedApps(apps)

        val mostUsedPackages = mostUsedApps
            .map { it.packageName }
            .toSet()

        val remainingApps = apps.filter {
            it.packageName !in mostUsedPackages
        }

        appList.value = mostUsedApps + remainingApps
        showUsageDialog.value = false
    } else {
        appList.value = apps

        if (!showUsageDialog.value) {
            showUsageDialog.value = true
        }
    }
}

private fun hasUsageAccess(): Boolean {
    val appOpsManager =
        getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager

    return appOpsManager.checkOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        android.os.Process.myUid(),
        packageName
    ) == AppOpsManager.MODE_ALLOWED
}

private fun getMostUsedApps(
    apps: List<LaunchableApp>
): List<LaunchableApp> {
    val usageStatsManager =
        getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    val endTime = System.currentTimeMillis()
    val startTime =
        endTime - (30L * 24L * 60L * 60L * 1000L)

    val usageStats = try {
        usageStatsManager.queryAndAggregateUsageStats(
            startTime,
            endTime
        )
    } catch (_: SecurityException) {
        return emptyList()
    }

    return apps
        .mapNotNull { app ->
            val stats = usageStats[app.packageName]
                ?: return@mapNotNull null

            if (stats.totalTimeInForeground <= 0L) {
                return@mapNotNull null
            }

            app to stats.totalTimeInForeground
        }
        .sortedWith(
            compareByDescending<Pair<LaunchableApp, Long>> {
                it.second
            }.thenBy(
                String.CASE_INSENSITIVE_ORDER
            ) {
                it.first.label
            }
        )
        .take(6)
        .map { it.first }
}

private fun openUsageAccessSettings() {
    try {
        startActivity(
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        )
    } catch (_: Exception) {
        // Der Launcher bleibt auch ohne Nutzungszugriff funktionsfähig.
    }
}

private fun launchApp(packageName: String) {
    val intent =
        packageManager.getLaunchIntentForPackage(packageName)

    if (intent != null) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }
}

}

@Composable
private fun MinimalLauncher(
apps: List<LaunchableApp>,
usageAccessAvailable: Boolean,
showUsageDialog: Boolean,
onLaunch: (String) -> Unit,
onOpenUsageSettings: () -> Unit,
onDismissUsageDialog: () -> Unit
) {
val fontFamily = FontFamily(
Font(
R.font.manrope_light,
FontWeight.Light
)
)

MaterialTheme {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.Top,
            contentPadding =
                androidx.compose.foundation.layout.PaddingValues(
                    top = 28.dp,
                    bottom = 28.dp
                )
        ) {
            if (usageAccessAvailable && apps.isNotEmpty()) {

                items(
                    items = apps.take(6),
                    key = { it.packageName }
                ) { app ->
                    AppItem(
                        app = app,
                        fontFamily = fontFamily,
                        onLaunch = onLaunch
                    )
                }

                if (apps.size > 6) {
                    item {
                        Spacer(
                            modifier = Modifier.height(18.dp)
                        )
                    }

                    items(
                        items = apps.drop(6),
                        key = { it.packageName }
                    ) { app ->
                        AppItem(
                            app = app,
                            fontFamily = fontFamily,
                            onLaunch = onLaunch
                        )
                    }
                }

            } else {

                items(
                    items = apps,
                    key = { it.packageName }
                ) { app ->
                    AppItem(
                        app = app,
                        fontFamily = fontFamily,
                        onLaunch = onLaunch
                    )
                }
            }
        }
    }

    if (showUsageDialog) {
        AlertDialog(
            onDismissRequest = onDismissUsageDialog,
            title = {
                Text("Meistgenutzte Apps")
            },
            text = {
                Text(
                    "SR Minimal Text Launcher kann deine " +
                        "meistgenutzten Apps oben anzeigen. " +
                        "Dafür benötigt Android einmalig den " +
                        "Zugriff auf die Nutzungsdaten."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = onOpenUsageSettings
                ) {
                    Text("Öffnen")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismissUsageDialog
                ) {
                    Text("Später")
                }
            }
        )
    }
}

}

@Composable
private fun AppItem(
app: LaunchableApp,
fontFamily: FontFamily,
onLaunch: (String) -> Unit
) {
Text(
text = app.label,
color = Color.White,
fontFamily = fontFamily,
fontSize = 18.sp,
fontWeight = FontWeight.Light,
modifier = Modifier
.fillMaxWidth()
.clickable {
onLaunch(app.packageName)
}
.padding(vertical = 9.dp)
)
}
