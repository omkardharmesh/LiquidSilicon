package com.liquidglass

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
public actual fun rememberDeviceTiltLight(baseAngleDegrees: Float): GlassLight {
    val context = LocalContext.current
    val light = remember(baseAngleDegrees) { TiltGlassLight(baseAngleDegrees) }
    DisposableEffect(context, light) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val magnitude = kotlin.math.sqrt(x * x + y * y + z * z)
                light.onUpVector(x, -y, magnitude)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        if (sensorManager != null && sensor != null) {
            sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        }
        onDispose { sensorManager?.unregisterListener(listener) }
    }
    return light
}
