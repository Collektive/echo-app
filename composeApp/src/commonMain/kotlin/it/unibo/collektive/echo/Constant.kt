package it.unibo.collektive.echo

/**
 * Default max propagation distance in meters.
 */
const val DEFAULT_MAX_DISTANCE = 1000.0

/**
 * Default max propagation time in seconds.
 */
const val DEFAULT_MAX_TIME = 60.0

/**
 * Minimum propagation distance in meters.
 */
const val MIN_DISTANCE = 100.0

/**
 * Minimum propagation time in seconds.
 */
const val MIN_TIME = 10.0

/**
 * Maximum propagation distance in meters.
 */
const val MAX_DISTANCE = 50000.0

/**
 * Maximum propagation time in seconds.
 */
const val MAX_TIME = 120.0

/**
 * Port number of MQTT broker.
 */
const val PORT_NUMBER_BROKER = 1883

/**
 * MQTT host.
 */
const val MQTT_HOST = "broker.hivemq.com"

/**
 * WebSocket endpoint (null for plain TCP MQTT).
 */
val WEBSOCKET_ENDPOINT: String? = null
