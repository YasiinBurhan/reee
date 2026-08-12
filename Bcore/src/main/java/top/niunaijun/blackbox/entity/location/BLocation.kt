package top.niunaijun.blackbox.entity.location

import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable

class BLocation : Parcelable {
    var latitude: Double = 0.0
        private set
    var longitude: Double = 0.0
        private set
    private var mAltitude: Double = 0.0
    private var mSpeed: Float = 0.0f
    private var mBearing: Float = 0.0f
    private var mAccuracy: Float = 0.0f

    constructor()

    constructor(latitude: Double, longitude: Double) {
        this.latitude = latitude
        this.longitude = longitude
    }

    constructor(parcel: Parcel) {
        this.latitude = parcel.readDouble()
        this.longitude = parcel.readDouble()
        this.mAltitude = parcel.readDouble()
        this.mAccuracy = parcel.readFloat()
        this.mSpeed = parcel.readFloat()
        this.mBearing = parcel.readFloat()
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeDouble(this.latitude)
        dest.writeDouble(this.longitude)
        dest.writeDouble(this.mAltitude)
        dest.writeFloat(this.mSpeed)
        dest.writeFloat(this.mBearing)
        dest.writeFloat(this.mAccuracy)
    }

    override fun describeContents(): Int = 0

    fun isEmpty(): Boolean = latitude == 0.0 && longitude == 0.0

    fun convert2SystemLocation(): Location {
        val location = Location(LocationManager.GPS_PROVIDER)
        location.latitude = latitude
        location.longitude = longitude
        location.speed = mSpeed
        location.bearing = mBearing
        location.accuracy = 40f
        location.time = System.currentTimeMillis()
        val extraBundle = Bundle()
        val satelliteCount = 10
        extraBundle.putInt("satellites", satelliteCount)
        extraBundle.putInt("satellitesvalue", satelliteCount)
        location.extras = extraBundle
        return location
    }

    override fun toString(): String {
        return "BLocation{latitude: $latitude, longitude: $longitude, altitude: $mAltitude, speed: $mSpeed, bearing: $mBearing, accuracy: $mAccuracy}"
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<BLocation> = object : Parcelable.Creator<BLocation> {
            override fun createFromParcel(source: Parcel): BLocation = BLocation(source)
            override fun newArray(size: Int): Array<BLocation?> = arrayOfNulls(size)
        }
    }
}
