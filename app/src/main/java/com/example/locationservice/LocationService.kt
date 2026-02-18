package com.example.locationservice

import android.app.*
import android.content.Intent
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*

class LocationService : Service() {

    private lateinit var fusedClient: FusedLocationProviderClient
    private lateinit var userId: String

    override fun onCreate() {
        super.onCreate()

        userId = Settings.Secure.getString(
            applicationContext.contentResolver,
            Settings.Secure.ANDROID_ID
        )

        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        startForeground(1, buildNotification())
        startLocationUpdates()
    }

    private fun buildNotification(): Notification {
        val channelId = "location_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Konum Servisi",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Konum aktif")
            .setContentText("Arka planda konum gönderiliyor")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()
    }

    private fun startLocationUpdates() {
        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000
        ).build()

        fusedClient.requestLocationUpdates(
            request,
            object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val loc = result.lastLocation ?: return
                    sendLocation(loc)
                }
            },
            mainLooper
        )
    }

    private fun getAddress(lat: Double, lon: Double): String {
        return try {
            val geocoder = Geocoder(this, Locale("tr"))
            val list = geocoder.getFromLocation(lat, lon, 1)

            if (!list.isNullOrEmpty()) {
                val a = list[0]
                listOfNotNull(
                    a.thoroughfare,
                    a.subThoroughfare,
                    a.subLocality,
                    a.locality
                ).joinToString(", ")
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }

private fun sendLocation(location: Location) {
    Thread {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val time = sdf.format(Date())

            // ===== ADRES AL =====
            var addressText = "Adres alınamadı"
            try {
                val geocoder = Geocoder(this, Locale.getDefault())
                val list = geocoder.getFromLocation(
                    location.latitude,
                    location.longitude,
                    1
                )
                if (!list.isNullOrEmpty()) {
                    addressText = list[0].getAddressLine(0) ?: addressText
                }
            } catch (_: Exception) {}

            // 🔥 URL ENCODE (EN KRİTİK NOKTA)
            val encodedAddress = URLEncoder.encode(addressText, "UTF-8")

            val postData =
                "uid=$userId" +
                "&lat=${location.latitude}" +
                "&lon=${location.longitude}" +
                "&speed=${location.speed}" +
                "&time=$time" +
                "&address=$encodedAddress"

            val url = URL("https://melipos.com/location_receiver/konum.php")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = 5000
            conn.readTimeout = 5000

            val writer = OutputStreamWriter(conn.outputStream)
            writer.write(postData)
            writer.flush()
            writer.close()

            conn.inputStream.close()

        } catch (_: Exception) {}
    }.start()
}



    override fun onBind(intent: Intent?): IBinder? = null
}


