package it.unibo.collektive.echo

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
