package com.example.locationservice

import android.Manifest
import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import android.util.Log

class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val channelId = "LocationServiceChannel"

    override fun onCreate() {
        super.onCreate()

        // --- Foreground notification ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Konum Servisi",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Konum Servisi")
            .setContentText("Konum servisiniz çalışıyor")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()

        // --- Runtime izin kontrolü ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.FOREGROUND_SERVICE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            stopSelf()
            return
        }

        startForeground(1, notification)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        startLocationUpdates()
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            interval = 5000
            fastestInterval = 3000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.locations.forEach { location ->
                        saveLocationToFile(location)   
                        sendLocationToServer(location) // POST artık doğru klasöre gidiyor
                    }
                }
            },
            mainLooper
        )
    }

    private fun saveLocationToFile(location: Location) {
        try {
            // Private storage’da txt oluşturur, uygulama kendisi görebilir
            val file = File(filesDir, "location.txt")
            val output = FileOutputStream(file, true)
            output.write("${location.latitude},${location.longitude},${System.currentTimeMillis()}\n".toByteArray())
            output.close()

            Log.d("LocationService", "location.txt path: ${file.absolutePath}")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun sendLocationToServer(location: Location) {
        Thread {
            try {
                // ✅ DİKKAT: Burada sunucu klasör adı doğru yazıldı
                val url = URL("https://melipos.com/location_receiver/konum.php") // kendi sunucu URL’in
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
