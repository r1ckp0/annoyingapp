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

    // ... (otras propiedades como MY_PERMISSIONS_REQUEST_PACKAGE_USAGE_STATS)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Botón de ejemplo para iniciar/detener el monitoreo (opcional)
        // val toggleButton: Button = findViewById(R.id.toggle_monitoring_button)
        // toggleButton.setOnClickListener {
        //     if (isServiceRunning(AppMonitorService::class.java)) { // Necesitarías una función para chequear esto
        //         stopAppMonitorService()
        //         toggleButton.text = "Iniciar Monitor"
        //     } else {
        //         checkAndStartMonitoring()
        //     }
        // }

        // Comprobar y solicitar permisos al inicio, si no se usa un botón
        checkAndStartMonitoring()
    }

    // También en checkAndStartMonitoring, si el permiso ya estaba concedido:
    private fun checkAndStartMonitoring() {
        if (!hasUsageStatsPermission()) {
            requestUsageStatsPermission()
        } else {
            Log.d("MainActivity", "Permiso de UsageStats ya concedido.")
            startAppMonitorService()
            // Aquí también cerramos la actividad
            finish() // Esto cerrará MainActivity si el permiso ya estaba dado al abrir la app
        }
    }

    // hasUsageStatsPermission() y requestUsageStatsPermission() como las tenías

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == MY_PERMISSIONS_REQUEST_PACKAGE_USAGE_STATS) {
            if (hasUsageStatsPermission()) {
                Log.d("MainActivity", "Permiso de UsageStats concedido por el usuario.")
                startAppMonitorService()
                // Aquí cerramos la actividad después de iniciar el servicio
                finish() // Esto cerrará MainActivity
            } else {
                Log.w("MainActivity", "Permiso de UsageStats no concedido por el usuario.")
                // Aquí podrías mostrar un diálogo explicando que la app no puede funcionar
                // y luego quizás también llamar a finish(), o darle otra oportunidad.
                // Por ejemplo, un Toast y luego cerrar:
                Toast.makeText(this, "Permiso necesario no concedido. La aplicación se cerrará.", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun startAppMonitorService() {
        Log.d("MainActivity", "Intentando iniciar AppMonitorService.")
        val serviceIntent = Intent(this, AppMonitorService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun stopAppMonitorService() {
        Log.d("MainActivity", "Intentando detener AppMonitorService.")
        stopService(Intent(this, AppMonitorService::class.java))
    }

    // La función getCurrentForegroundApp() en MainActivity ahora es opcional.
    // Podrías mantenerla si quieres, por ejemplo, tener un botón en tu UI que
    // muestre la app actual en primer plano COMO UNA ACCIÓN PUNTUAL,
    // pero NO se usaría para el MONITOREO CONTINUO.
    /*
    private fun getCurrentForegroundApp(): String? {
        if (!hasUsageStatsPermission()) {
            Log.w("UsageStats", "No se puede obtener la app en primer plano: permiso denegado.")
            return null
        }
        // ... (resto de la implementación como la tenías) ...
        Log.d("ForegroundApp", "Current App (desde MainActivity - llamada puntual): $currentApp")
        return currentApp
    }
    */

    // (Opcional) Función para verificar si el servicio está corriendo
    // private fun isServiceRunning(serviceClass: Class<*>): Boolean {
    //     val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    //     for (service in manager.getRunningServices(Int.MAX_VALUE)) {
    //         if (serviceClass.name == service.service.className) {
    //             return true
    //         }
    //     }
    //     return false
    // }
}