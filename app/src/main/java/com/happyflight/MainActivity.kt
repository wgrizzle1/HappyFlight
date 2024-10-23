package com.happyflight

import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.happyflight.databinding.ActivityMainBinding
//custom imports
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import android.content.pm.PackageManager
import android.Manifest
import android.widget.Button
import android.widget.EditText
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import android.widget.Toast
import androidx.navigation.fragment.NavHostFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(R.layout.activity_main)

        // Get the NavController
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController


        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.

        // Initialize the location client

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Check if the permission is already granted, else request it
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED) {
        } else {
            requestLocationPermission()
        }

        // Initialize views
        val radiusInput = findViewById<EditText>(R.id.radiusInput)
        val applyRadiusButton = findViewById<Button>(R.id.applyRadiusButton)
        // Set OnClickListener for the Apply Radius button
        applyRadiusButton.setOnClickListener {
            val radiusText = radiusInput.text.toString()
            if (radiusText.isNotEmpty()) {
                try {
                    val radius = radiusText.toDouble()  // Convert to double
                    getCurrentLocation(radius)  // Call location function with radius
                } catch (e: NumberFormatException) {
                    Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Radius cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun requestLocationPermission() {
        requestPermissionsLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
        }
        else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getCurrentLocation(radiusInMiles: Double) {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestLocationPermission()
            return  // Stop further execution if permission isn't granted
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val latitude = location.latitude
                val longitude = location.longitude
                Toast.makeText(this, "Lat: $latitude, Lon: $longitude", Toast.LENGTH_LONG).show()

                val openskyURL = buildOpenSkyUrl(latitude, longitude, radiusInMiles)
                println(latitude)
                println(longitude)
                println(openskyURL)
            } else {
                Toast.makeText(this, "Failed to get location", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun buildOpenSkyUrl(lat: Double, lon: Double, radiusInMiles: Double): String {
        // Constants for latitude and longitude conversion
        val MILES_PER_LATITUDE_DEGREE = 69.0  // 1 degree of latitude = ~69 miles
        val MILES_PER_LONGITUDE_DEGREE_AT_EQUATOR = 69.0  // At equator, 1 degree = ~69 miles

        // Calculate the latitude offset
        val latOffset = radiusInMiles / MILES_PER_LATITUDE_DEGREE

        // Calculate the longitude offset (adjusted for the current latitude)
        val milesPerLongitudeDegree = MILES_PER_LONGITUDE_DEGREE_AT_EQUATOR * Math.cos(Math.toRadians(lat))
        val lonOffset = radiusInMiles / milesPerLongitudeDegree

        // Build the bounding box based on the offsets
        val lamin = lat - latOffset
        val lamax = lat + latOffset
        val lomin = lon - lonOffset
        val lomax = lon + lonOffset

        val formattedURL = "https://opensky-network.org/api/states/all?lamin=$lamin&lomin=$lomin&lamax=$lamax&lomax=$lomax"
        // Return the formatted API URL
        return formattedURL
    }

}