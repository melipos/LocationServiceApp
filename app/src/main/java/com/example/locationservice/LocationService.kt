package com.example.locationservice

import android.app.*
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.*

class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    override fun onCreate() {
        super.onCreate()

        startForeground(1, createNotification())

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            10_000
        ).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {

                // 🔥 HATA BURADAYDI – location BURADA TANIMLANIYOR
                for (location in result.locations) {
                    sendLocation(
                        location.latitude,
                        location.longitude
                    )
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            request,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun sendLocation(lat: Double, lon: Double) {
        Thread {
            try {
                val url = java.net.URL(
                    "https://melipos.com/location_receiver/location_receiver.php" +
                            "?latitude=$lat&longitude=$lon"
                )

                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                conn.connect()
                conn.disconnect()

            } catch (e: Exception) {
                Log.e("SERVER", "SEND ERROR", e)
            }
        }.start()
    }

    override fun onDestroy() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        super.onDestroy()
    }

    override fun onBind(intent: android.content.Intent?): IBinder? = null

    private fun createNotification(): Notification {
        val channelId = "location_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Location Service",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }

        return Notification.Builder(this, channelId)
            .setContentTitle("Konum Servisi")
            .setContentText("Konum gönderiliyor")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .build()
    }
}
