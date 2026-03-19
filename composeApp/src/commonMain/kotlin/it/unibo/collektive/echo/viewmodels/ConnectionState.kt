package it.unibo.collektive.echo.viewmodels

/** Represents the MQTT broker connection state. */
enum class ConnectionState {
    /** Successfully connected to the broker. */
    CONNECTED,

    /** Not connected to the broker. */
    DISCONNECTED,

    /** Actively broadcasting a message. */
    SENDING,
}
