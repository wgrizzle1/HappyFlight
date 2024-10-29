package com.happyflight

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomsheet.BottomSheetDialog
import android.widget.TextView
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import org.json.JSONArray
import com.happyflight.Flight


class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var radiusInput: EditText
    private lateinit var trackedFlightsButton: Button

    // Declare the BroadcastReceiver at the class level
    private lateinit var planeUpdateReceiver: BroadcastReceiver

    companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 1
        const val NOTIFICATION_PERMISSION_REQUEST_CODE = 2
        const val PLANE_UPDATE_ACTION = "com.happyflight.PLANE_UPDATE" // Ensure consistency here
        const val TRACKED_FLIGHTS_UPDATE_ACTION = "com.happyflight.TRACKED_FLIGHTS_UPDATE"

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Location Services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Set up the radius input and buttons
        radiusInput = findViewById(R.id.radiusInput)
        val startTrackingButton = findViewById<Button>(R.id.startTrackingButton)
        val stopTrackingButton = findViewById<Button>(R.id.stopTrackingButton)

        // Initialize the tracked flights button
        trackedFlightsButton = findViewById(R.id.trackedFlightsButton)

        // Set up navigation to the Tracked Flights page
        trackedFlightsButton.setOnClickListener {
            val intent = Intent(this, TrackedFlightsActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            }
            startActivity(intent)  // Navigate to the tracked flights page
        }


        // Initialize and register the BroadcastReceiver
        initializeBroadcastReceiver()

        // Start tracking planes when the button is clicked
        startTrackingButton.setOnClickListener {
            if (checkPermissions()) {
                startTrackingService()
            } else {
                requestPermissions()
            }
        }

        // Stop tracking planes when the button is clicked
        stopTrackingButton.setOnClickListener {
            stopTrackingService()
        }

        // Request necessary permissions on app launch
        requestPermissions()
    }

    private fun initializeBroadcastReceiver() {
        planeUpdateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val planesInfo = intent.getStringExtra("planesInfo")
                println("Broadcast received with planesInfo: $planesInfo")
                if (planesInfo != null) {
                    showBottomSheet(planesInfo)  // Display the plane info
                } else {
                    println("Received broadcast but no plane info found.")
                }
            }
        }

        // Register the receiver with LocalBroadcastManager
        val filter = IntentFilter(PLANE_UPDATE_ACTION)
        LocalBroadcastManager.getInstance(this).registerReceiver(planeUpdateReceiver, filter)
    }
    private var dialog: BottomSheetDialog? = null  // Maintain a single instance

    private fun showBottomSheet(planesInfo: String) {
        // Dismiss the existing dialog safely
        dialog?.let { if (it.isShowing) it.dismiss() }

        // Inflate the bottom sheet layout
        val bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_planes, null)

        // Create a new BottomSheetDialog and assign it to the `dialog` property
        val newDialog = BottomSheetDialog(this, R.style.NoDimBottomSheetDialogTheme).apply {
            setContentView(bottomSheetView)

            // Allow dismissal on outside touch by default
            setCanceledOnTouchOutside(true)

            window?.setDimAmount(0f)  // Disable dimming

            // Populate the TextView with the plane info
            val textView = bottomSheetView.findViewById<TextView>(R.id.planesInfo)
            textView.text = buildPlaneDisplayText(planesInfo)

            // Handle clicks outside the bottom sheet content to dismiss the dialog
            bottomSheetView.setOnClickListener {
                // No action here, prevent accidental dismissal while interacting with content
            }

            findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.setOnClickListener {
                // No-op: Ensures clicks inside the bottom sheet content aren't dismissed
            }

            // Set a touch listener on the background to dismiss the dialog when clicked outside
            setOnCancelListener { dismiss() }
        }

        // Assign the new dialog to the `dialog` property and show it
        dialog = newDialog
        newDialog.show()
    }



    private fun buildPlaneDisplayText(planesInfo: String): String {
        val stringBuilder = StringBuilder()

        try {
            // Parse the JSON string into a JSONArray
            val planesArray = JSONArray(planesInfo)

            for (i in 0 until planesArray.length()) {
                val planeObject = planesArray.getJSONObject(i)

                // Extract relevant information
                val callSign = planeObject.optString("callsign", "No Call Sign").trim()
                val distance = String.format("%.2f", planeObject.optDouble("distance_in_miles", 0.0))
                val altitude = planeObject.optInt("geo_altitude", 0)
                val velocity = planeObject.optInt("velocity", 0)
                val vrate = planeObject.optInt("vertical_rate", 0)

                // Build the formatted text for each plane
                stringBuilder.append("Plane ${i + 1}: $callSign\nDistance (in miles): $distance\nAltitude: $altitude\nVelocity: $velocity\nVertical Change Rate: $vrate\n\n")
            }
        } catch (e: Exception) {
            // Handle JSON parsing exceptions
            stringBuilder.append("Error parsing plane data: ${e.message}")
        }

        return stringBuilder.toString()
    }


    override fun onDestroy() {
        super.onDestroy()
        // Unregister the receiver with LocalBroadcastManager
        LocalBroadcastManager.getInstance(this).unregisterReceiver(planeUpdateReceiver)
    }

    private fun requestPermissions() {
        val locationPermissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (locationPermissions.any {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
        ) {
            ActivityCompat.requestPermissions(
                this, locationPermissions, LOCATION_PERMISSION_REQUEST_CODE
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Location permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }

        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkPermissions(): Boolean {
        val locationPermissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        val allGranted = locationPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (!allGranted) {
            Toast.makeText(this, "Please grant location permissions", Toast.LENGTH_SHORT).show()
        }

        return allGranted
    }


    private fun startTrackingService() {
        if (checkPermissions()) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val latitude = location.latitude
                    val longitude = location.longitude

                    val radiusText = radiusInput.text.toString()
                    val radius = radiusText.toDoubleOrNull() ?: 25.0

                    val intent = Intent(this, PlaneTrackingService::class.java).apply {
                        putExtra("latitude", latitude)
                        putExtra("longitude", longitude)
                        putExtra("radius", radius)
                    }

                    startForegroundService(intent)  // Start the foreground service
                    Toast.makeText(this, "Started tracking planes", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to get location", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener { exception ->
                Toast.makeText(this, "Error retrieving location: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            requestPermissions()  // Request permissions if not granted
        }
    }


    private fun stopTrackingService() {
        val intent = Intent(this, PlaneTrackingService::class.java)
        stopService(intent)  // Stop the service
        Toast.makeText(this, "Stopped tracking planes", Toast.LENGTH_SHORT).show()
    }
}
