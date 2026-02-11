package com.example.locationservice

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Location Service", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Location Service")
            .setContentText("Tracking location in background")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()

        startForeground(1, notification)
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L).build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    val location = locationResult.lastLocation ?: return
                    sendLocationToServer(location.latitude, location.longitude)
                }
            },
            mainLooper
        )
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


