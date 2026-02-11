package com.example.locationservice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.google.android.gms.location.*
import okhttp3.*
import java.io.IOException

class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val client = OkHttpClient()

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        startForegroundService()
        startLocationUpdates()
    }

    private fun startForegroundService() {
        val channelId = "location_service_channel"
        val channelName = "Location Service"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = Notification.Builder(this, channelId)
            .setContentTitle("Location Service")
            .setContentText("Tracking location in background")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()

        startForeground(1, notification)
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 60000L).build()

        fusedLocationClient.requestLocationUpdates(locationRequest, object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                val location = locationResult.lastLocation ?: return
                Log.d("LocationService", "Lat: ${location.latitude}, Lon: ${location.longitude}")
                postLocation(location.latitude, location.longitude)
            }
        }, mainLooper)
    }

    private fun postLocation(lat: Double, lon: Double) {
        val url = "https://yourserver.com/location"
        val body = FormBody.Builder()
            .add("latitude", lat.toString())
            .add("longitude", lon.toString())
            .build()

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("LocationService", "Failed to post location", e)
            }

            override fun onResponse(call: Call, response: Response) {
                Log.d("LocationService", "Location posted successfully")
            }
        })
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
