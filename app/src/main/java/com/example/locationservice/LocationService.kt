package com.example.locationservice

import android.Manifest
import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.*
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val channelId = "location_service"

    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Konum Servisi",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Konum Takibi")
            .setContentText("Konum gönderiliyor")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()

        startForeground(1, notification)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        startLocationUpdates()
    }

    private fun startLocationUpdates() {
        if (
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000
        ).build()

        fusedLocationClient.requestLocationUpdates(
            request,
            object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val location = result.lastLocation ?: return
                    saveToFile(location)
                    postLocation(location)
                }
            },
            mainLooper
        )
    }

    private fun saveToFile(location: Location) {
        val file = File(filesDir, "location.txt")
        val line = "${location.latitude},${location.longitude},${System.currentTimeMillis()}\n"
        FileOutputStream(file, true).use {
            it.write(line.toByteArray())
        }
    }

    private fun postLocation(location: Location) {
        Thread {
            try {
                val url = URL("https://melipos.com/location_receiver/konum.php")
                val data = "lat=${location.latitude}&lon=${location.longitude}"

                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.outputStream.write(data.toByteArray())
                conn.outputStream.close()
                conn.inputStream.close()
            } catch (_: Exception) {}
        }.start()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
