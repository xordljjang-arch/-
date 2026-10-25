package kr.busan.galmaetgil.pedometer

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle

class GalmaetgilGpsTracker(private val context: Context) : LocationListener {

    private var location: Location? = null
    var latitude: Double = 0.0
    var longitude: Double = 0.0
    private val locationManager: LocationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    init {
        getLocation()
    }

    @SuppressLint("MissingPermission")
    private fun getLocation(): Location? {
        try {
            val isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            if (isGPSEnabled || isNetworkEnabled) {
                if (isNetworkEnabled) {
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10000L, 10f, this)
                    location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    location?.let {
                        latitude = it.latitude
                        longitude = it.longitude
                    }
                }
                if (isGPSEnabled) {
                    if (location == null) {
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000L, 10f, this)
                        location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                        location?.let {
                            latitude = it.latitude
                            longitude = it.longitude
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return location
    }

    fun canGetLocation(): Boolean {
        val isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        return isGPSEnabled || isNetworkEnabled
    }

    override fun onLocationChanged(loc: Location) {
        location = loc
        latitude = loc.latitude
        longitude = loc.longitude
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
}
