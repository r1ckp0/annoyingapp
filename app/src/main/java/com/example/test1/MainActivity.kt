package com.example.test1

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.app.usage.UsageStatsManager
import android.util.Log
import java.util.SortedMap
import java.util.TreeMap

class MainActivity : AppCompatActivity() {

    private val MY_PERMISSIONS_REQUEST_PACKAGE_USAGE_STATS = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // Asumiendo que tienes un layout activity_main.xml

        if (!hasUsageStatsPermission()) {
            requestUsageStatsPermission()
        } else {
            // Permiso ya concedido, puedes empezar a monitorear
            startMonitoring()
        }
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                packageName
            )
        } else {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun requestUsageStatsPermission() {
        // Si el permiso no está concedido, lleva al usuario a la pantalla de configuración
        startActivityForResult(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS), MY_PERMISSIONS_REQUEST_PACKAGE_USAGE_STATS)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == MY_PERMISSIONS_REQUEST_PACKAGE_USAGE_STATS) {
            if (hasUsageStatsPermission()) {
                // Permiso concedido después de que el usuario interactuó con la configuración
                startMonitoring()
            } else {
                // Permiso no concedido. Podrías mostrar un mensaje al usuario.
                Log.w("UsageStats", "Usage stats permission not granted.")
            }
        }
    }

    private fun startMonitoring() {
        // Aquí es donde implementarías la lógica para verificar la app en primer plano
        // Esto podría ser en un servicio en segundo plano
        Log.d("UsageStats", "Permission granted. Monitoring can start.")
        // Por ahora, solo un ejemplo de cómo obtener la app actual
        getCurrentForegroundApp()
    }

    // Función de ejemplo para obtener la app en primer plano (simplificada)
    private fun getCurrentForegroundApp(): String? {
        if (!hasUsageStatsPermission()) return null

        var currentApp: String? = null
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val time = System.currentTimeMillis()
        // Consultamos eventos de los últimos 10 segundos, por ejemplo
        val appList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 10, time)
        if (appList != null && appList.isNotEmpty()) {
            val mySortedMap: SortedMap<Long, String> = TreeMap()
            for (usageStats in appList) {
                mySortedMap[usageStats.lastTimeUsed] = usageStats.packageName
            }
            if (mySortedMap.isNotEmpty()) {
                currentApp = mySortedMap[mySortedMap.lastKey()]
                Log.d("ForegroundApp", "Current App: $currentApp")
            }
        }
        return currentApp
    }
    private fun startAppMonitorService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(Intent(this, AppMonitorService::class.java))
        } else {
            startService(Intent(this, AppMonitorService::class.java))
        }
    }

    private fun stopAppMonitorService() {
        stopService(Intent(this, AppMonitorService::class.java))
    }
}