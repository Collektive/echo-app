package it.unibo.collektive.echo.location

actual fun createLocationService(): LocationService = PlatformLocationService()
