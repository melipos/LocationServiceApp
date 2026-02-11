package com.example.locationservice

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.google.android.gms.location.*
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    // 🔴 PHP DOSYANIN TAM URL'Sİ
    private val SERVER_URL = "https://melipos.com/location_receiver/get_locations.php"

    override fun onCreate() {
        super.onCreate()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            10000 // 10 saniye
        ).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (location in result.locations) {
                    val lat = location.latitude
                    val lon = location.longitude

                    Log.d("LocationService", "LAT=$lat LON=$lon")
                    sendLocationToServer(lat, lon)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            mainLooper
        )
    }

    private fun sendLocationToServer(lat: Double, lon: Double) {
        thread {
            try {
                val url = URL(SERVER_URL)
                val conn = url.openConnection() as HttpURLConnection

                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                conn.doOutput = true
                conn.connectTimeout = 10000
                conn.readTimeout = 10000

                val postData = "lat=$lat&lon=$lon"

                conn.outputStream.use {
                    it.write(postData.toByteArray())
                }

                val responseCode = conn.responseCode
                Log.d("LocationService", "SERVER RESPONSE: $responseCode")

                conn.disconnect()

            } catch (e: Exception) {
                Log.e("LocationService", "POST ERROR", e)
            }
        }
    }

    override fun onDestroy() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
