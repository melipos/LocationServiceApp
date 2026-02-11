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
                for (location in result.locations) {
                    if (BuildConfig.DEBUG) {
                    Log.d(
                        "LOCATION",
                        "Lat:${location.latitude} Lon:${location.longitude}"
                    )
                    }
                    sendLocationToServer(
                        location.latitude,
                        location.longitude
                    )
                }
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


    private fun sendLocationToServer(lat: Double, lon: Double) {
        Thread {
            try {
                val client = OkHttpClient()

                val body = FormBody.Builder()
                    .add("latitude", lat.toString())
                    .add("longitude", lon.toString())
                    .build()

                val request = Request.Builder()
                    .url("https://melipos.com/location_receiver/location_receiver.php")
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
                Log.d("SERVER", "Response: ${response.body?.string()}")

            } catch (e: Exception) {
                Log.e("SERVER", "POST ERROR", e)
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


