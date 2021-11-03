package com.example.geoo

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.geoo.databinding.ActivityMapsBinding
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.Marker

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var geofencingClient: GeofencingClient

    companion object {
        const val REQUEST_LOCATION_PERMISSION = 1
        const val UWI_LAT = 18.006372
        const val UWI_LONG = -76.750096
        const val UWI_KEY = "10101"
        const val UWI_RADIUS_FENCE = 1000F
        const val UWI_GEOFENCE_EXPIRATION = 100000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        geofencingClient = LocationServices.getGeofencingClient(this)
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Add a marker in Sydney and move the camera
        val uwi_doc = LatLng(UWI_LAT, UWI_LONG)
        val markerOptions = MarkerOptions()
            .position(uwi_doc)
            .title("UWI - Department of Computing")
        val marker:Marker? = mMap.addMarker(markerOptions)
        if (marker != null) {
            //always show the marker label on the screen after adding it
            marker.showInfoWindow()
        }
        mMap.moveCamera(CameraUpdateFactory.newLatLng(uwi_doc))

        //setting the zoom level - no animation
        mMap.moveCamera(CameraUpdateFactory.zoomTo(2.0F))

        //demonstrating the ability to animate the zoom using the API
        mMap.animateCamera(CameraUpdateFactory.zoomTo(15.0F), 2000, null)

        //enable the rendering of 3D buildings in the area of focus
        mMap.isBuildingsEnabled = true

        //to display traffic in the area of focus
        mMap.isTrafficEnabled = true

        //allow zoom controls to be displayed on the screen

        mMap.uiSettings.isZoomControlsEnabled = true

        //enable setting to allow for turn by turn directions to the location
        mMap.uiSettings.isMapToolbarEnabled = true

        //enable two finger titling of the world in focus
        mMap.uiSettings.isTiltGesturesEnabled = true

        //initialize the location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        //finally enable location tracking and display on Google Maps
        enableMyLocation()


        findViewById<Button>(R.id.get_location_button).setOnClickListener {
            checkLastLocation()
        }

        findViewById<Button>(R.id.setup_button).setOnClickListener {
            createGeofence()
        }

    }

    /**
     * Check if the permission has been granted by user for accessing FINE_LOCATION
     **/
    private fun isPermissionGranted(): Boolean {
        return ContextCompat
            .checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check first if the user has the correct permissions and if not, ask permission to enable
     * the location services.
     */
    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        if(isPermissionGranted()) {
            mMap.isMyLocationEnabled = true
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf<String>(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION)
        }
    }

    /**
     * After the user has either granted or revoked the location permissions this method will
     * be called with the supplied request code from the enableMyLocation() function.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if( requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.contains(PackageManager.PERMISSION_GRANTED)) {
                enableMyLocation()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    @SuppressLint("MissingPermission")
    private fun checkLastLocation() {
        if(isPermissionGranted()) {
            fusedLocationClient.lastLocation.addOnSuccessListener {
                location:Location? ->
                Toast.makeText(this, "Last Known Location: ${location?.latitude}, ${location?.longitude}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val geofencePendingIntent: PendingIntent by lazy{
        val intent = Intent(this, GeofenceBroadcastReceiver::class.java)

        PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }


    /**
     * Setup a location around the UWI location.
     */
    @SuppressLint("MissingPermission")
    private fun createGeofence() {
        if(isPermissionGranted()) {
            var fence = Geofence.Builder()
                .setRequestId(UWI_KEY)
                .setCircularRegion(UWI_LAT, UWI_LONG, UWI_RADIUS_FENCE)
                .setExpirationDuration(UWI_GEOFENCE_EXPIRATION)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                .build()

            val geofenceRequest = GeofencingRequest.Builder().apply {
                setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                addGeofence(fence)
            }.build()

            geofencingClient?.addGeofences(geofenceRequest, geofencePendingIntent)?.run {
                addOnSuccessListener {
                    Toast.makeText(
                        applicationContext,
                        "Geofence added around UWI - DOC",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                addOnFailureListener {
                    Toast.makeText(
                        applicationContext,
                        "Geofence failed to be added around UWI - DOC",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e("Geoo", it.toString())
                }
            }
        }
    }
}