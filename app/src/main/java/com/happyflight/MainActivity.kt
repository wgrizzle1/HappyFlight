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
import android.content.Context
import android.content.ClipboardManager
import android.content.ClipData
import org.chromium.net.CronetEngine
import org.chromium.net.UrlRequest
import org.chromium.net.UrlResponseInfo
import org.chromium.net.CronetException
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.nio.ByteBuffer
import org.json.JSONObject
import java.nio.charset.Charset
import kotlin.math.*
import android.os.Handler
import android.os.Looper

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(R.layout.activity_main)

        val myBuilder = CronetEngine.Builder(this)
        val cronetEngine: CronetEngine = myBuilder.build()



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
                    getCurrentLocation(radius,cronetEngine)  // Call location function with radius
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

    private fun getCurrentLocation(radiusInMiles: Double, cronetEngine: CronetEngine) {
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

                val openskyURL = buildOpenSkyUrl(latitude, longitude, radiusInMiles,cronetEngine)
                println(latitude)
                println(longitude)
                println(openskyURL)
            } else {
                Toast.makeText(this, "Failed to get location", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun buildOpenSkyUrl(lat: Double, lon: Double, radiusInMiles: Double, cronetEngine: CronetEngine): String {
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

        // Gets a handle to the clipboard service.
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip: ClipData = ClipData.newPlainText("simple text", formattedURL)
        // Set the clipboard's primary clip.
        clipboard.setPrimaryClip(clip)

        val executor: Executor = Executors.newSingleThreadExecutor()
        val requestBuilder = cronetEngine.newUrlRequestBuilder(
            formattedURL,
            MyUrlRequestCallback(this,lat,lon),
            executor
        )
        val request: UrlRequest = requestBuilder.build()
        request.start()


        // Return the formatted API URL
        return formattedURL
    }
}
private const val TAG = "MyUrlRequestCallback"

class MyUrlRequestCallback(private val context: Context, private val userLat: Double, private val userLon: Double) : UrlRequest.Callback() {
    private val responseBody = StringBuilder()
    override fun onRedirectReceived(request: UrlRequest?, info: UrlResponseInfo?, newLocationUrl: String?) {
        println( "onRedirectReceived method called.")
        // You should call the request.followRedirect() method to continue
        // processing the request.
        request?.followRedirect()
    }

    override fun onResponseStarted(request: UrlRequest?, info: UrlResponseInfo?) {
        println("onResponseStarted method called.")
        // You should call the request.read() method before the request can be
        // further processed. The following instruction provides a ByteBuffer object
        // with a capacity of 102400 bytes for the read() method. The same buffer
        // with data is passed to the onReadCompleted() method.
        request?.read(ByteBuffer.allocateDirect(102400))
    }

    override fun onReadCompleted(
        request: UrlRequest,
        info: UrlResponseInfo,
        byteBuffer: ByteBuffer?
    ) {
        byteBuffer?.let { buffer ->
            buffer.flip() // Switch buffer to read mode

            // Read the data from the buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)  // Copy the buffer content into the byte array

            // Append the data to the responseBody (assuming responseBody is a class-level variable)
            responseBody.append(String(bytes, Charset.forName("UTF-8")))

            println("Read completed: ${bytes.size} bytes")

            buffer.clear()  // Clear the buffer for the next read
            request.read(buffer)  // Continue reading the response
        } ?: run {
            println("ByteBuffer is null.")
        }
    }

    override fun onSucceeded(request: UrlRequest?, info: UrlResponseInfo?) {
        println("onSucceeded method called.")

        try {
            // Parse the response body into a JSONObject
            val jsonResponse = JSONObject(responseBody.toString())
            println("Parsed JSON: $jsonResponse")

            // Extract the 'states' array from the JSONObject
            val respArray = jsonResponse.optJSONArray("states")

            if (respArray != null) {
                val handler = Handler(Looper.getMainLooper())  // Create a Handler for the main UI thread
                var delay = 0L  // Start with 0ms delay
                // Iterate through the outer array
                for (i in 0 until respArray.length()) {

                    val innerArray = respArray.getJSONArray(i)  // Get each inner array
                    var callSign = innerArray.get(1) as String
                    callSign = callSign.trim()
                    if (callSign.isEmpty()) {
                        callSign = "No Call Sign"
                    }

                    val planeLat = innerArray.get(6) as Double
                    val planeLon = innerArray.get(5) as Double

                    var planeDistance = haversine(userLat,userLon,planeLat,planeLon)
                    planeDistance = String.format("%.2f", planeDistance).toDouble()
                    val iAdjusted = i+1
                    val msg = "Aircraft $iAdjusted: $callSign, Distance: $planeDistance" + "km"
                    // Schedule the Toast to show with a 1-second interval between each
                    handler.postDelayed({
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }, delay)

                    delay += 1000  // Increment the delay by 1 second (1000 ms) for each Toast

                    println(msg)

                }
            } else {
                println("No 'states' array found in the JSON response.")
            }
        } catch (e: Exception) {
            println("Failed to parse JSON: ${e.message}")
        }
    }
    // Called if the request fails.
    override fun onFailed(
        request: UrlRequest,
        info: UrlResponseInfo,
        error: CronetException
    ) {
        println("Request failed: ${error.message}")
    }

    fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371.0  // Earth's radius in kilometers

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return R * c  // Distance in kilometers
    }
}