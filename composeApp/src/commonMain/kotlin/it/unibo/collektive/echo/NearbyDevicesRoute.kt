package it.unibo.collektive.echo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import it.unibo.collektive.echo.location.LocationPermissionHandler
import it.unibo.collektive.echo.ui.NearbyDevicesScreen
import it.unibo.collektive.echo.viewmodels.NearbyDevicesViewModel
import kotlin.uuid.ExperimentalUuidApi

/** Handles permission and lifecycle concerns before rendering the nearby-devices UI. */
@OptIn(ExperimentalUuidApi::class)
@Composable
fun NearbyDevicesRoute(modifier: Modifier = Modifier) {
    var viewModel by remember { mutableStateOf<NearbyDevicesViewModel?>(null) }

    LocationPermissionHandler(
        onPermissionGranted = { locationService ->
            if (viewModel == null) {
                viewModel = NearbyDevicesViewModel(locationService = locationService)
            }
        },
        onPermissionDenied = {
            viewModel = null
        },
    )

    viewModel?.let { vm ->
        val uiState by vm.uiState.collectAsState()

        LaunchedEffect(vm) {
            vm.startLocationTracking()
            vm.startCollektiveProgram()
        }

        DisposableEffect(vm) {
            onDispose(vm::cleanup)
        }

        NearbyDevicesScreen(
            modifier = modifier,
            uiState = uiState,
            onSendMessage = vm::sendMessage,
            onMessageParametersChange = vm::updateMessageParameters,
        )
    }
}
