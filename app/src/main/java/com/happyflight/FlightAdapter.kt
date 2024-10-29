package com.happyflight

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

class FlightAdapter(private val context: Context, private val flights: List<Flight>) : BaseAdapter() {

    override fun getCount(): Int = flights.size

    override fun getItem(position: Int): Any = flights[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View = convertView ?: LayoutInflater.from(context).inflate(
            R.layout.item_flight, parent, false
        )

        val flight = flights[position]

        // Bind data to the UI elements
        val callSignText = view.findViewById<TextView>(R.id.callSignText)
        val statusText = view.findViewById<TextView>(R.id.statusText)

        callSignText.text = "Call Sign: ${flight.callSign}"
        statusText.text = if (flight.isLanded) "Status: Landed" else "Status: In Air"

        return view
    }
}
