package com.example.locationservice

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.*
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build


class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    override fun onCreate() {
        super.onCreate()
         startForeground(1, createNotification())
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            10_000
        ).setMinUpdateIntervalMillis(5_000).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
            sendLocationToServerSimple(
            location.latitude,
            location.longitude
            )

            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun createNotification(): Notification {
    val channelId = "location_channel"

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            channelId,
            "Location Service",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    return Notification.Builder(this, channelId)
        .setContentTitle("Konum Servisi Çalışıyor")
        .setContentText("Konum gönderiliyor")
        .setSmallIcon(android.R.drawable.ic_menu_mylocation)
        .build()
}

    
    private fun sendLocationToServerSimple(lat: Double, lon: Double) {
    Thread {
        try {
            val url = java.net.URL(
                "https://siteadresin.com/location_receiver.php" +
                        "?latitude=$lat&longitude=$lon"
            )

            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.connect()

            val code = conn.responseCode
            conn.disconnect()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }.start()
}


    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}



