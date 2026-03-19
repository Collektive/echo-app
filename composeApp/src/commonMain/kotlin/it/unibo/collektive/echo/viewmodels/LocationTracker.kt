package it.unibo.collektive.echo.viewmodels

import com.diamondedge.logging.logging
import it.unibo.collektive.echo.location.Location
import it.unibo.collektive.echo.location.LocationError
import it.unibo.collektive.echo.location.LocationService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Handles GPS bootstrap and streaming updates independent from the UI state holder. */
class LocationTracker(
    private val scope: CoroutineScope,
    private val locationService: LocationService,
    private val onLocationChanged: (Location) -> Unit,
    private val onLocationError: (LocationError) -> Unit,
    private val onMailboxLocationUpdate: (Location) -> Unit = {},
) {
    private val log = logging("LOCATION-TRACKER")
    private var trackingJob: Job? = null

    fun start() {
        if (trackingJob?.isActive == true) {
            return
        }

        trackingJob = scope.launch {
            log.i { "Starting location tracking..." }
            delay(LOCATION_STARTUP_DELAY_MS)

            try {
                val initialLocation = locationService.getCurrentLocation()
                    ?: throw LocationError.ServiceUnavailable

                publishLocation(initialLocation)

                locationService.startLocationUpdates { location ->
                    publishLocation(location)
                }
            } catch (error: LocationError) {
                onLocationError(error)
                logLocationError(error)
            } catch (error: CancellationException) {
                throw error
            } catch (
                @Suppress("TooGenericExceptionCaught")
                error: RuntimeException,
            ) {
                onLocationError(LocationError.Unknown(error))
                log.e { "Unexpected GPS error: ${error.message}" }
            }
        }
    }

    fun stop() {
        trackingJob?.cancel()
        trackingJob = null
        locationService.stopLocationUpdates()
        log.i { "GPS tracking stopped" }
    }

    private fun publishLocation(location: Location) {
        onLocationChanged(location)
        onMailboxLocationUpdate(location)
        log.i {
            "Location update: ${location.latitude}, ${location.longitude} (accuracy: ${location.accuracy}m)"
        }
    }

    private fun logLocationError(error: LocationError) {
        log.e { "Location error - GPS is mandatory for this app: ${error::class.simpleName}" }
        when (error) {
            is LocationError.PermissionDenied -> log.e {
                "GPS permission denied - app cannot function without location access"
            }

            is LocationError.LocationDisabled -> log.e {
                "GPS services disabled - app requires GPS to be enabled"
            }

            is LocationError.ServiceUnavailable -> log.e { "GPS service unavailable - app cannot function" }

            is LocationError.Unknown -> log.e { "Unknown GPS error: ${error.cause?.message}" }
        }
    }

    private companion object {
        const val LOCATION_STARTUP_DELAY_MS = 1000L
    }
}
