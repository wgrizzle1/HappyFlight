package com.happyflight

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import org.json.JSONArray

class TrackedFlightsActivity : AppCompatActivity() {

    private val trackedFlights = mutableListOf<Flight>()
    private lateinit var flightAdapter: FlightAdapter

    private val flightUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val planesInfoString = intent.getStringExtra("planesInfo")
            println("Received broadcast with planesInfo: $planesInfoString")
            if (planesInfoString != null) {
                try {
                    val planesArray = JSONArray(planesInfoString)
                    val updatedFlights = mutableListOf<Flight>()

                    for (i in 0 until planesArray.length()) {
                        val flightJson = planesArray.getJSONObject(i)
                        val flight = Flight(
                            callSign = flightJson.getString("callSign"),
                            isLanded = flightJson.getBoolean("isLanded"),
                            latitude = flightJson.getDouble("latitude"),
                            longitude = flightJson.getDouble("longitude"),
                            distanceInMiles = flightJson.getDouble("distanceInMiles"),
                            altitude = flightJson.getInt("altitude"),
                            velocity = flightJson.getInt("velocity"),
                            verticalRate = flightJson.getInt("verticalRate")
                        )
                        updatedFlights.add(flight)
                    }

                    trackedFlights.clear()
                    trackedFlights.addAll(updatedFlights)
                    flightAdapter.notifyDataSetChanged()

                } catch (e: Exception) {
                    println("Error parsing planesInfo: ${e.message}")
                }
            }
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tracked_flights)

        // Initialize the ListView and Adapter
        val flightListView = findViewById<ListView>(R.id.flightListView)
        flightAdapter = FlightAdapter(this, trackedFlights)
        flightListView.adapter = flightAdapter

        // Register the receiver for flight updates
        val filter = IntentFilter(MainActivity.TRACKED_FLIGHTS_UPDATE_ACTION)
        LocalBroadcastManager.getInstance(this).registerReceiver(flightUpdateReceiver, filter)

        println("TrackedFlightsActivity created and receiver is registered")
    }


    override fun onDestroy() {
        println("TrackedFlightsActivity is being destroyed, unregistering receiver")
        super.onDestroy()
        // Unregister the receiver to avoid memory leaks
        LocalBroadcastManager.getInstance(this).unregisterReceiver(flightUpdateReceiver)
    }
}

