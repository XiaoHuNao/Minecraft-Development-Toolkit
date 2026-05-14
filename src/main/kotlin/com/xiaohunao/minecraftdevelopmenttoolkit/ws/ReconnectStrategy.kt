package com.xiaohunao.minecraftdevelopmenttoolkit.ws

import kotlin.math.min
import kotlin.random.Random

class ReconnectStrategy(
    private val maxDelayMs: Long = 30_000L,
    private val initialDelayMs: Long = 1_000L
) {
    private var attempt = 0

    fun nextDelay(): Long {
        val base = min(initialDelayMs * (1L shl attempt), maxDelayMs)
        val jitter = (base * 0.2 * Random.nextDouble(-1.0, 1.0)).toLong()
        attempt++
        return maxOf(500L, base + jitter)
    }

    fun reset() {
        attempt = 0
    }

    fun getAttempt(): Int = attempt
}
