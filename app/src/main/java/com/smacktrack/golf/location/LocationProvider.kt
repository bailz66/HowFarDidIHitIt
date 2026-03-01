package com.smacktrack.golf.location

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * A single location fix from [FusedLocationProviderClient].
 */
data class LocationUpdate(
    val lat: Double,
    val lon: Double,
    val accuracyMeters: Float,
    val timestampMs: Long
)

/**
 * Wraps [FusedLocationProviderClient] to emit a [Flow] of [LocationUpdate]s.
 *
 * Requests high-accuracy GPS fixes at a configurable interval. The flow
 * automatically removes location callbacks when the collector is cancelled,
 * preventing GPS battery drain.
 *
 * Callers must ensure `ACCESS_FINE_LOCATION` permission is granted before
 * collecting from [locationUpdates] — the `@SuppressLint("MissingPermission")`
 * annotation is justified because [com.smacktrack.golf.MainActivity] checks
 * permissions before the ViewModel starts location updates.
 */
class LocationProvider(context: Context) {

    private val client: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    fun locationUpdates(intervalMs: Long = 500L): Flow<LocationUpdate> = callbackFlow {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .setMinUpdateIntervalMillis(intervalMs)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (location in result.locations) {
                    trySend(
                        LocationUpdate(
                            lat = location.latitude,
                            lon = location.longitude,
                            accuracyMeters = location.accuracy,
                            timestampMs = location.time
                        )
                    )
                }
            }
        }

        client.requestLocationUpdates(request, callback, Looper.getMainLooper())

        awaitClose {
            client.removeLocationUpdates(callback)
        }
    }
}
