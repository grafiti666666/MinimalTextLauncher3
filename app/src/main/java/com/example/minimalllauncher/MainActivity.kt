package com.example.minimalllauncher

import android.content.Intent
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import java.util.Locale

data class LaunchableApp(
    val label: String,
    val packageName: String
)

class MainActivity : ComponentActivity() {

    private val appList = mutableStateOf<List<LaunchableApp>>(emptyList())
    private val batteryLevel = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.BLACK
        window.navigationBarColor = android.graphics.Color.BLACK

        loadApps()
        updateBattery()

        setContent {
            MinimalLauncher(
                apps = appList.value,
                battery = batteryLevel.intValue,
                onLaunch = { packageName -> launchApp(packageName) }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        updateBattery()
        loadApps()
    }

    private fun updateBattery() {
        val manager = getSystemService(BATTERY_SERVICE) as BatteryManager
        batteryLevel.intValue = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    private fun loadApps() {
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val apps = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .map {
                LaunchableApp(
                    label = it.loadLabel(pm).toString(),
                    packageName = it.activityInfo.packageName
                )
            }
            .distinctBy { it.packageName }
            .filter { it.packageName != packageName }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })

        appList.value = apps
    }

    private fun launchApp(packageName: String) {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
    }
}

@Composable
private fun MinimalLauncher(
    apps: List<LaunchableApp>,
    battery: Int,
    onLaunch: (String) -> Unit
) {
    // Manrope is a free, open-source font with a clean geometric appearance.
    // Google Fonts supplies it at runtime/cache; the system falls back gracefully.
    val fontFamily = FontFamily(
        Font("sans-serif", FontWeight.Normal)
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
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    top = 28.dp,
                    bottom = 28.dp
                )
            ) {
                item {
                    Text(
                        text = battery.coerceIn(0, 100).toString(),
                        color = Color.White,
                        fontFamily = fontFamily,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Normal,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 28.dp)
                    )
                }

                items(
                    items = apps,
                    key = { it.packageName }
                ) { app ->
                    Text(
                        text = app.label,
                        color = Color.White,
                        fontFamily = fontFamily,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Normal,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLaunch(app.packageName) }
                            .padding(vertical = 9.dp)
                    )
                }
            }
        }
    }
}
