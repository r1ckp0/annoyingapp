package com.example.test1

// En un nuevo archivo, por ejemplo, AppMonitorService.kt
import android.app.AppOpsManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import java.util.SortedMap
import java.util.TreeMap

class AppMonitorService : Service() {

    private val NOTIFICATION_ID = 1
    private val CHANNEL_ID = "AppMonitorChannel"
    private val TAG = "AppMonitorService"

    private lateinit var handler: Handler
    private lateinit var runnable: Runnable

    private val ACTION_STOP_SERVICE = "com.yourapp.action.STOP_SERVICE" // Define u

    // Lista de paquetes de redes sociales (ejemplos, debes completarla)
    private val socialMediaPackages = listOf(
        "com.facebook.katana", // Facebook
        "com.instagram.android", // Instagram
        "com.twitter.android", // Twitter / X
        "com.whatsapp", // WhatsApp
        "com.zhiliaoapp.musically", // TikTok
        "com.ss.android.ugc.trill",  // TikTok (otra variante)
        "com.google.android.youtube"
        // Agrega más según necesites
    )

    override fun onCreate() {
        super.onCreate()
        handler = Handler(Looper.getMainLooper())
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Primero, verificar si el intent tiene nuestra acción para detener el servicio
        if (intent?.action == ACTION_STOP_SERVICE) {
            Log.d(TAG, "Acción de detener servicio recibida.")
            stopSelf() // Detiene el servicio
            return START_NOT_STICKY // Opcional: no reiniciar si se detiene explícitamente
        }

        createNotificationChannel() // Asegúrate de que el canal esté creado

        // Intent para cuando se toque la notificación (puede abrir MainActivity o nada)
        // Si no quieres que haga nada al tocar el cuerpo de la notificación, puedes
        // crear un PendingIntent vacío o que no haga nada.
        val notificationIntent = Intent(this, MainActivity::class.java) // Opcional
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, pendingIntentFlags)

        // Intent para la acción de "Detener"
        val stopServiceIntent = Intent(this, AppMonitorService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopServicePendingIntent = PendingIntent.getService(
            this,
            1, // Request code diferente al del content intent
            stopServiceIntent,
            pendingIntentFlags // Reusamos los mismos flags
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Monitor de 'Disturbios' Activo")
            .setContentText("Vigilando aplicaciones para tu proyecto artístico.")
            .setSmallIcon(R.mipmap.ic_launcher) // Reemplaza con tu ícono
            .setContentIntent(pendingIntent) // Acción al tocar la notificación (opcional)
            .setOngoing(true) // Hace la notificación no descartable (ya lo hace startForeground, pero es bueno ser explícito)
            .addAction(R.drawable.ic_stop_placeholder, "Detener Monitor", stopServicePendingIntent) // ¡ACCIÓN DE DETENER!
            // Necesitarás crear un ícono 'ic_stop_placeholder.xml' en tu carpeta res/drawable
            // (puede ser un simple ícono de stop)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }


        // Lógica de monitoreo (handler y runnable) como antes
        runnable = Runnable {
            checkForegroundApp()
            handler.postDelayed(runnable, 1000) // Verificar cada segundo
        }
        handler.post(runnable)

        Log.d(TAG, "Servicio iniciado con notificación y acción de detener.")
        return START_STICKY
    }

    private fun checkForegroundApp() {
        val currentApp = getCurrentForegroundAppPackageName(this)
        if (currentApp != null && socialMediaPackages.contains(currentApp)) {
            Log.i(TAG, "¡Aplicación de red social detectada!: $currentApp")
            // --- AQUÍ LLAMARÍAS A TUS FUNCIONES DE "DISTURBIO" ---
            // Ejemplo:
            // disturbExperience()
        }
    }

    private fun getCurrentForegroundAppPackageName(context: Context): String? {
        var currentApp: String? = null
        // Verifica el permiso de nuevo, aunque debería estar concedido si el servicio corre
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        } else {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        }
        if (mode != AppOpsManager.MODE_ALLOWED) {
            Log.w(TAG, "Usage stats permission not granted in service.")
            return null // No se puede obtener sin permiso
        }


        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val time = System.currentTimeMillis()
        val appList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 2, time) // Un intervalo corto

        if (appList != null && appList.isNotEmpty()) {
            val mySortedMap: SortedMap<Long, String> = TreeMap()
            for (usageStats in appList) {
                mySortedMap[usageStats.lastTimeUsed] = usageStats.packageName
            }
            if (mySortedMap.isNotEmpty()) {
                currentApp = mySortedMap[mySortedMap.lastKey()]
            }
        }
        return currentApp
    }


    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnable) // Detener el monitoreo
        Log.d(TAG, "Servicio destruido")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // No necesitamos binding para este tipo de servicio
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "App Monitor Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    // --- FUNCIONES DE DISTURBIO (EJEMPLOS) ---
    // private fun disturbExperience() {
    //    showWaitScreen()
    //    vibrateDevice()
    //    playSound()
    // }

    // private fun showWaitScreen() {
    //    // Para esto, podrías lanzar una actividad de tu app que ocupe toda la pantalla
    //    // y tenga un temporizador. Necesitarás el permiso SYSTEM_ALERT_WINDOW
    //    // o encontrar una forma menos intrusiva si quieres evitar ese permiso.
    //    // Este permiso es muy sensible y tiene restricciones en Play Store.
    //    // Alternativa: Mostrar una notificación de alta prioridad que llame la atención.
    //    Log.d(TAG, "Mostrando pantalla de espera (simulado)")
    // }

    // private fun vibrateDevice() {
    //    // Necesitas <uses-permission android:name="android.permission.VIBRATE" />
    //    // val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    //    // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    //    //    vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
    //    // } else {
    //    //    vibrator.vibrate(500)
    //    // }
    //    Log.d(TAG, "Vibrando dispositivo (simulado)")
    // }

    // private fun playSound() {
    //    // Usa MediaPlayer o SoundPool para reproducir un sonido
    //    // Puedes controlar el volumen con AudioManager
    //    Log.d(TAG, "Reproduciendo sonido molesto (simulado)")
    // }
}