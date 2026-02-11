package com.example.locationservice

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mapView: MapView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var googleMap: GoogleMap? = null
    private var isFirstFix = true


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val intent = Intent(this, LocationService::class.java)
        startForegroundService(intent)

        // MapView init
        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // İzinleri sor ve servis + konum güncellemeyi başlat
        requestPermissions()
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }

        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->
            // Tüm izinler verilmişse
            if (result.values.all { it }) {
                startService(Intent(this, LocationService::class.java))
                startLocationUpdates()
            }
        }

        requestPermissionLauncher.launch(permissions.toTypedArray())
    }

override fun onMapReady(map: GoogleMap) {
    googleMap = map
    googleMap?.uiSettings?.isZoomControlsEnabled = true

    try {
        googleMap?.isMyLocationEnabled = true
    } catch (e: SecurityException) {
        e.printStackTrace()
    }
}


private fun startLocationUpdates() {
    val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY,
        3000L
    ).build()

    fusedLocationClient.requestLocationUpdates(
        locationRequest,
        object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation ?: return

                val latLng = LatLng(location.latitude, location.longitude)

                googleMap?.let { map ->
                    map.clear()
                    map.addMarker(
                        MarkerOptions().position(latLng).title("Konumun")
                    )

                    // 📌 SADECE İLK KONUMDA YAKINLAŞ
                    if (isFirstFix) {
                        map.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(latLng, 18f)
                        )
                        isFirstFix = false
                    }
                }
            }
        },
        mainLooper
    )
}

    // MapView lifecycle methods
    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause() { super.onPause(); mapView.onPause() }
    override fun onDestroy() { super.onDestroy(); mapView.onDestroy() }
    override fun onLowMemory() { super.onLowMemory(); mapView.onLowMemory() }
}





