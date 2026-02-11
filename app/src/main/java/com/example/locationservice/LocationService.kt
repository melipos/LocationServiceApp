package com.example.locationservice

import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.IBinder
import android.util.Log
import com.google.android.gms.location.*
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class LocationService : Service() {

    private lateinit var fusedClient: FusedLocationProviderClient
    private lateinit var callback: LocationCallback

    private val SERVER_URL = "https://melipos.com/location_receiver/konum.php"

    private var lastLocation: Location? = null
    private var lastMoveTime = System.currentTimeMillis()

    override fun onCreate() {
        super.onCreate()

        fusedClient = LocationServices.getFusedLocationProviderClient(this)

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            10000
        ).build()

        callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                processLocation(location)
            }
        }

        fusedClient.requestLocationUpdates(request, callback, mainLooper)
    }

    private fun processLocation(loc: Location) {
        val lat = loc.latitude
        val lon = loc.longitude
        val speed = loc.speed   // m/s

        var isStop = 0

        if (speed < 0.5) {
            if (System.currentTimeMillis() - lastMoveTime > 120000) {
                isStop = 1
            }
        } else {
            lastMoveTime = System.currentTimeMillis()
        }

        lastLocation = loc
        sendToServer(lat, lon, speed, isStop)
    }

    private fun sendToServer(
        lat: Double,
        lon: Double,
        speed: Float,
        isStop: Int
    ) {
        thread {
            try {
                val url = URL(SERVER_URL)
                val conn = url.openConnection() as HttpURLConnection

                conn.requestMethod = "POST"
                conn.setRequestProperty(
                    "Content-Type",
                    "application/x-www-form-urlencoded"
                )
                conn.doOutput = true
                conn.connectTimeout = 10000
                conn.readTimeout = 10000

                val postData =
                    "lat=$lat&lon=$lon&speed=$speed&stop=$isStop"

                conn.outputStream.use {
                    it.write(postData.toByteArray())
                }

                Log.d("LocationService", "POST OK ${conn.responseCode}")
                conn.disconnect()

            } catch (e: Exception) {
                Log.e("LocationService", "POST ERROR", e)
            }
        }
    }

    override fun onDestroy() {
        fusedClient.removeLocationUpdates(callback)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
