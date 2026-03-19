package it.unibo.collektive.echo.viewmodels

import it.unibo.collektive.echo.location.Location
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Utility for converting neighbor GPS coordinates into Haversine distances. */
object NeighborDistanceCalculator {
    private const val EarthRadiusMeters = 6371000.0
    private const val DegreesHalfCircle = 180.0

    fun calculateDistance(
        fromLatitude: Double,
        fromLongitude: Double,
        toLatitude: Double,
        toLongitude: Double,
    ): Double {
        val deltaLatitude = (toLatitude - fromLatitude) * PI / DegreesHalfCircle
        val deltaLongitude = (toLongitude - fromLongitude) * PI / DegreesHalfCircle
        val a = sin(deltaLatitude / 2).pow(2) +
            cos(fromLatitude * PI / DegreesHalfCircle) *
            cos(toLatitude * PI / DegreesHalfCircle) *
            sin(deltaLongitude / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return EarthRadiusMeters * c
    }

    @OptIn(ExperimentalUuidApi::class)
    fun calculateDistances(
        currentLocation: Location,
        selfId: Uuid,
        neighborIds: Iterable<Uuid>,
        neighborLocationProvider: (Uuid) -> Location?,
        onDiagnostic: (String) -> Unit = {},
    ): Map<Uuid, Double> {
        val distances = mutableMapOf<Uuid, Double>()

        neighborIds.forEach { neighborId ->
            when {
                neighborId == selfId -> onDiagnostic("Excluding self ($neighborId) from neighbor calculations")
                else -> {
                    val neighborLocation = neighborLocationProvider(neighborId)
                    if (neighborLocation == null) {
                        onDiagnostic("Neighbor $neighborId GPS data not available yet - excluding from distance calculation")
                    } else {
                        distances[neighborId] = calculateDistance(
                            fromLatitude = currentLocation.latitude,
                            fromLongitude = currentLocation.longitude,
                            toLatitude = neighborLocation.latitude,
                            toLongitude = neighborLocation.longitude,
                        )
                    }
                }
            }
        }

        return distances
    }
}
