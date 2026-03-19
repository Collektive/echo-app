package it.unibo.collektive.echo.viewmodels

import it.unibo.collektive.echo.location.Location
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class NeighborDistanceCalculatorTest {

    @Test
    fun `calculateDistance returns zero for identical coordinates`() {
        val distance = NeighborDistanceCalculator.calculateDistance(
            fromLatitude = 44.4949,
            fromLongitude = 11.3426,
            toLatitude = 44.4949,
            toLongitude = 11.3426,
        )

        assertEquals(0.0, distance, absoluteTolerance = 0.0001)
    }

    @OptIn(ExperimentalUuidApi::class)
    @Test
    fun `calculateDistances skips self and neighbors without location`() {
        val selfId = Uuid.random()
        val neighborId = Uuid.random()
        val missingNeighborId = Uuid.random()
        val currentLocation = Location(latitude = 44.4949, longitude = 11.3426)
        val neighborLocation = Location(latitude = 44.4938, longitude = 11.3387)

        val distances = NeighborDistanceCalculator.calculateDistances(
            currentLocation = currentLocation,
            selfId = selfId,
            neighborIds = listOf(selfId, neighborId, missingNeighborId),
            neighborLocationProvider = { id ->
                if (id == neighborId) {
                    neighborLocation
                } else {
                    null
                }
            },
        )

        assertEquals(1, distances.size)
        assertTrue(distances.containsKey(neighborId))
    }
}
