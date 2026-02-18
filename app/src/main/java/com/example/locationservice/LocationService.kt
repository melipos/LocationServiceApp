package com.example.locationservice

import android.app.*
import android.content.Context
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val channelId = "LocationServiceChannel"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForegroundServiceNotification()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        startLocationUpdates()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Konum Servisi",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundServiceNotification() {
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Konum Servisi")
            .setContentText("Konum servisiniz çalışıyor")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()
        startForeground(1, notification)
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            interval = 5000
            fastestInterval = 3000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        // Battery optimization ignore
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                intent.data = Uri.parse("package:$packageName")
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.locations.forEach { location ->
                        saveLocationToFile(location)
                        sendLocationToServer(location)  // Sunucuya POST
                    }
                }
            },
            Looper.getMainLooper()
        )
    }

    private fun saveLocationToFile(location: Location) {
        try {
            val file = File(filesDir, "location.txt")
            val output = FileOutputStream(file, true)
            output.write("${location.latitude},${location.longitude},${System.currentTimeMillis()}\n".toByteArray())
            output.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun sendLocationToServer(location: Location) {
        Thread {
            try {
                val url = URL("https://melipos.com/location_receiver/konum.php") // kendi URL’in
                val postData = "lat=${location.latitude}&lon=${location.longitude}"
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.outputStream.write(postData.toByteArray())
                conn.outputStream.flush()
                conn.outputStream.close()
                conn.inputStream.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

