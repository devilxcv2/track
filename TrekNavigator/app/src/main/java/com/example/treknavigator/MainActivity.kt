// MainActivity.kt
/*
 * Activity principale con UI, GPS, routing, caching e progress.
 */
package com.example.treknavigator

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.maplibre.gl.geometry.LatLng
import com.maplibre.gl.maps.MapView
import com.maplibre.gl.maps.MapboxMap
import com.maplibre.gl.maps.Style
import com.maplibre.gl.style.layers.LineLayer
import com.maplibre.gl.style.layers.PropertyFactory.*
import com.maplibre.gl.style.sources.GeoJsonSource
import com.google.android.gms.location.*
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {
    private lateinit var mapView: MapView
    private var map: MapboxMap? = null
    private lateinit var fusedClient: FusedLocationProviderClient
    private val engineScope = CoroutineScope(Dispatchers.Main + Job())
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        progressBar = findViewById(R.id.progressBar)
        mapView = findViewById(R.id.mapView)
        fusedClient = LocationServices.getFusedLocationProviderClient(this)

        requestLocationPermission()

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync { mbMap ->
            map = mbMap
            mbMap.setStyle(Style.MAPBOX_STREETS) {
                it.addSource(GeoJsonSource("route-source"))
                it.addLayer(LineLayer("route-layer","route-source")
                    .withProperties(lineWidth(4f), lineColor("#ff0000")))
            }
        }

        val inputStart = findViewById<EditText>(R.id.inputStart)
        val inputEnd = findViewById<EditText>(R.id.inputEnd)
        val btnUseGPS = findViewById<Button>(R.id.buttonUseGPS)
        val btnFind = findViewById<Button>(R.id.buttonFind)
        val btnNavigate = findViewById<Button>(R.id.buttonNavigate)
        val tvDistance = findViewById<TextView>(R.id.textDistance)
        val tvElevation = findViewById<TextView>(R.id.textElevation)
        val tvTime = findViewById<TextView>(R.id.textTime)

        btnUseGPS.setOnClickListener {
            progressBar.visibility = View.VISIBLE
            fusedClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    inputStart.setText("${loc.latitude},${loc.longitude}")
                } else {
                    Toast.makeText(this,"GPS non disponibile",Toast.LENGTH_SHORT).show()
                }
                progressBar.visibility = View.GONE
            }
        }

        btnFind.setOnClickListener {
            engineScope.launch {
                progressBar.visibility = View.VISIBLE
                val startInp = inputStart.text.toString()
                val endInp = inputEnd.text.toString()
                val start = RouteEngine.resolveInput(startInp)
                val end = RouteEngine.resolveInput(endInp)
                if (start==null||end==null) {
                    Toast.makeText(this@MainActivity,"Input non valido",Toast.LENGTH_SHORT).show()
                    progressBar.visibility = View.GONE
                    return@launch
                }
                val result = withContext(Dispatchers.IO) {
                    val body = TrailDownloader.getPathsCached(
                        LatLonBounds(start.lat,start.lon,end.lat,end.lon), this@MainActivity)
                    RouteEngine.calculateRoute(start,end)
                }
                tvDistance.text="Distanza: %.2f km".format(result.distanceMeters/1000)
                tvElevation.text="Dislivello: %.0f m".format(result.elevationDiff)
                tvTime.text="Tempo: %.1f h".format(result.distanceMeters/1000/4.0)
                map?.getStyle {
                    val coords = result.path.map { LatLng(it.lat,it.lon) }
                    it.getSourceAs<GeoJsonSource>("route-source")
                        ?.setGeoJson(com.maplibre.geojson.LineString.fromLngLats(
                            coords.map{ com.maplibre.geojson.Point.fromLngLat(it.lon,it.lat) }
                        ))
                }
                progressBar.visibility = View.GONE
            }
        }

        btnNavigate.setOnClickListener {
            // Live camera follow
            fusedClient.requestLocationUpdates(
                LocationRequest.create().setInterval(5000).setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY),
                object : LocationCallback() {
                    override fun onLocationResult(res: LocationResult) {
                        val loc = res.lastLocation
                        map?.cameraPosition = com.maplibre.gl.camera.CameraPosition.Builder()
                            .target(LatLng(loc.latitude, loc.longitude))
                            .zoom(15.0)
                            .build()
                    }
                },
                mainLooper
            )
        }
    }

    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_FINE_LOCATION)) {
                AlertDialog.Builder(this)
                    .setTitle("Permesso Localizzazione")
                    .setMessage("Serve per ottenere la tua posizione su mappa")
                    .setPositiveButton("OK"){_,_->
                        ActivityCompat.requestPermissions(this,
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),1001)
                    }.show()
            } else {
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),1001)
            }
        }
    }

    override fun onStart() { super.onStart(); mapView.onStart() }
    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause() { mapView.onPause(); super.onPause() }
    override fun onStop() { mapView.onStop(); super.onStop() }
    override fun onDestroy() { mapView.onDestroy(); engineScope.cancel(); super.onDestroy() }
}
