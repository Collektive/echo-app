package it.unibo.collektive.echo.location

import androidx.compose.runtime.Composable

@Composable
expect fun LocationPermissionHandler(onPermissionGranted: (LocationService) -> Unit, onPermissionDenied: () -> Unit)
