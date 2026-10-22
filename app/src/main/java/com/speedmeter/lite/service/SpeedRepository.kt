package com.speedmeter.lite.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** جسر بين الخدمة والواجهة: السرعة اللحظية وحالة تشغيل الخدمة. */
object SpeedRepository {
    data class Speed(val down: Long = 0L, val up: Long = 0L)

    private val _speed = MutableStateFlow(Speed())
    val speed: StateFlow<Speed> = _speed.asStateFlow()

    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()

    fun update(down: Long, up: Long) { _speed.value = Speed(down, up) }
    fun setRunning(value: Boolean) {
        _running.value = value
        if (!value) _speed.value = Speed()
    }
}
