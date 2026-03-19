package it.unibo.collektive.echo.viewmodels

import it.unibo.collektive.echo.location.Location
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class NearbyDevicesStateStoreTest {

    @OptIn(ExperimentalUuidApi::class)
    @Test
    fun `dispatch updates state through reducer`() {
        val deviceId = Uuid.random()
        val stateStore = NearbyDevicesStateStore(deviceId)
        val location = Location(latitude = 44.4949, longitude = 11.3426)

        stateStore.dispatch(NearbyDevicesAction.ConnectionChanged(ConnectionState.CONNECTED))
        stateStore.dispatch(NearbyDevicesAction.LocationUpdated(location))

        val uiState = stateStore.uiState.value
        assertEquals(deviceId, uiState.deviceId)
        assertEquals(ConnectionState.CONNECTED, uiState.connection)
        assertEquals(location, uiState.currentLocation)
    }
}
