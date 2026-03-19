package it.unibo.collektive.echo

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import it.unibo.collektive.echo.ui.TopBar

/** Root composable that sets up the Material theme, scaffold, and top bar. */
@Composable
@Preview
fun App() {
    MaterialTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = { TopBar() },
        ) { innerPadding ->
            NearbyDevicesRoute(modifier = Modifier.padding(innerPadding))
        }
    }
}
