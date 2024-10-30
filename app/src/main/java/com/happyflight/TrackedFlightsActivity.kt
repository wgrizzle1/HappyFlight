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
            val receivedFlights = intent.getParcelableArrayListExtra<Flight>("trackedFlights")
            if (receivedFlights != null) {
                trackedFlights.clear()
                trackedFlights.addAll(receivedFlights)
                flightAdapter.notifyDataSetChanged()
                println("TrackedFlightsActivity updated with ${trackedFlights.size} flights")
            } else {
                println("No flights received in the broadcast.")
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

