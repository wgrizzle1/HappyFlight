package com.happyflight

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import org.chromium.net.CronetEngine
import org.chromium.net.UrlRequest
import org.chromium.net.UrlResponseInfo
import org.chromium.net.CronetException
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import org.json.JSONArray
import com.happyflight.Flight

class PlaneTrackingService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var cronetEngine: CronetEngine  // Initialize in onCreate
    private val executor = Executors.newSingleThreadExecutor()  // No delegation required

    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var radius: Double = 25.0

    companion object {
        const val CHANNEL_ID = "plane_tracking_channel"
        const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()

        // Initialize the CronetEngine in onCreate to avoid lazy delegation errors
        cronetEngine = CronetEngine.Builder(this).build()

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Tracking planes...", 0, ""))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        latitude = intent?.getDoubleExtra("latitude", 0.0) ?: 0.0
        longitude = intent?.getDoubleExtra("longitude", 0.0) ?: 0.0
        radius = intent?.getDoubleExtra("radius", 25.0) ?: 25.0

        startTrackingPlanes()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Plane Tracking Service",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(content: String, planesCount: Int, lastUpdate: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Tracking Planes Nearby")
            .setContentText("Planes Nearby: $planesCount | Last Update: $lastUpdate")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun startTrackingPlanes() {
        val runnable = object : Runnable {
            override fun run() {
                refreshPlaneData(latitude, longitude, radius)
                handler.postDelayed(this, 30000)  // Run every 30 seconds
            }
        }
        handler.post(runnable)
    }

    private val responseBody = StringBuilder()

    private fun refreshPlaneData(lat: Double, lon: Double, radius: Double) {
        val url = "https://happy-planes-58714a401586.herokuapp.com/planes?lat=$lat&lon=$lon&radius=$radius"
        println("Requesting URL: $url")

        val requestBuilder = cronetEngine.newUrlRequestBuilder(
            url,
            object : UrlRequest.Callback() {
                override fun onResponseStarted(request: UrlRequest?, info: UrlResponseInfo?) {
                    responseBody.setLength(0)  // Clear the response body for a new request
                    request?.read(ByteBuffer.allocateDirect(102400))  // Allocate a new buffer
                }

                override fun onReadCompleted(
                    request: UrlRequest?, info: UrlResponseInfo?, byteBuffer: ByteBuffer?
                ) {
                    byteBuffer?.let { buffer ->
                        buffer.flip()  // Switch to read mode
                        val bytes = ByteArray(buffer.remaining())
                        buffer.get(bytes)

                        responseBody.append(String(bytes, Charset.forName("UTF-8")))  // Append data

                        // Clear the buffer for reuse
                        buffer.clear()

                        // Continue reading if more data is available
                        request?.read(buffer)
                    }
                }

                override fun onSucceeded(request: UrlRequest?, info: UrlResponseInfo?) {
                    handlePlaneData(responseBody.toString())  // Parse the complete data
                }

                override fun onFailed(
                    request: UrlRequest?, info: UrlResponseInfo?, error: CronetException?
                ) {
                    println("Error: ${error?.message}")
                }

                override fun onRedirectReceived(
                    request: UrlRequest?, info: UrlResponseInfo?, newLocationUrl: String?
                ) {
                    request?.followRedirect()
                }
            },
            executor
        )
        requestBuilder.build().start()
    }

    private val trackedFlights = mutableListOf<Flight>()

    private fun handlePlaneData(planesInfo: String) {
        try {
            val planesArray = JSONArray(planesInfo)
            val newTrackedFlights = mutableListOf<Flight>()  // Temporary list for new data

            for (i in 0 until planesArray.length()) {
                val planeObject = planesArray.getJSONObject(i)

                // Extract flight data
                val callSign = planeObject.optString("callsign", "No Call Sign").trim()
                val isLanded = planeObject.optBoolean("on_ground", false)
                val latitude = planeObject.optDouble("latitude", 0.0)
                val longitude = planeObject.optDouble("longitude", 0.0)
                val distanceInMiles = planeObject.optDouble("distance_in_miles", 0.0)
                val altitude = planeObject.optInt("geo_altitude", 0)
                val velocity = planeObject.optInt("velocity", 0)
                val verticalRate = planeObject.optInt("vertical_rate", 0)

                // Create a new Flight object
                val flight = Flight(
                    callSign = callSign,
                    isLanded = isLanded,
                    latitude = latitude,
                    longitude = longitude,
                    distanceInMiles = distanceInMiles,
                    altitude = altitude,
                    velocity = velocity,
                    verticalRate = verticalRate
                )

                newTrackedFlights.add(flight)  // Add to the new list
            }

            // Update the tracked flights list
            trackedFlights.clear()
            trackedFlights.addAll(newTrackedFlights)

            // **Send the broadcast** with the updated tracked flights
            broadcastTrackedFlights()

            // Update the notification with the new count and time
            val notification = buildNotification(
                content = "Tracking planes...",
                planesCount = trackedFlights.size,
                lastUpdate = getCurrentTime()
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(NOTIFICATION_ID, notification)

        } catch (e: Exception) {
            println("Error parsing plane data: ${e.message}")
        }
    }



    private fun broadcastTrackedFlights() {
        val intent = Intent(MainActivity.TRACKED_FLIGHTS_UPDATE_ACTION)
        intent.putParcelableArrayListExtra("trackedFlights", ArrayList(trackedFlights))
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }




    private fun getCurrentTime(): String {
        val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return dateFormat.format(Date())
    }

    private fun sendPlaneUpdateBroadcast(trackedFlights: List<Flight>) {
        val intent = Intent(MainActivity.TRACKED_FLIGHTS_UPDATE_ACTION)
        intent.putParcelableArrayListExtra("trackedFlights", ArrayList(trackedFlights))
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)

        println("Broadcast sent with ${trackedFlights.size} flights")
    }




    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
