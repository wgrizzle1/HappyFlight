package com.happyflight

import android.os.Parcel
import android.os.Parcelable
import org.json.JSONObject

data class Flight(
    val callSign: String,
    val isLanded: Boolean,
    val latitude: Double,
    val longitude: Double,
    val distanceInMiles: Double,
    val altitude: Int,
    val velocity: Int,
    val verticalRate: Int
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readByte() != 0.toByte(),
        parcel.readDouble(),
        parcel.readDouble(),
        parcel.readDouble(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(callSign)
        parcel.writeByte(if (isLanded) 1 else 0)
        parcel.writeDouble(latitude)
        parcel.writeDouble(longitude)
        parcel.writeDouble(distanceInMiles)
        parcel.writeInt(altitude)
        parcel.writeInt(velocity)
        parcel.writeInt(verticalRate)
    }

    override fun describeContents(): Int = 0

    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("callSign", callSign)
            put("isLanded", isLanded)
            put("latitude", latitude)
            put("longitude", longitude)
            put("distanceInMiles", distanceInMiles)
            put("altitude", altitude)
            put("velocity", velocity)
            put("verticalRate", verticalRate)
        }
    }

    companion object CREATOR : Parcelable.Creator<Flight> {
        override fun createFromParcel(parcel: Parcel): Flight = Flight(parcel)
        override fun newArray(size: Int): Array<Flight?> = arrayOfNulls(size)
    }
}
