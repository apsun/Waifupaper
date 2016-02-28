package com.crossbowffs.waifupaper.app

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.service.wallpaper.WallpaperService
import net.rbgrn.android.glwallpaperservice.GLWallpaperService

class LiveWallpaperService : GLWallpaperService() {
    private lateinit var sensorManager: SensorManager
    private lateinit var sensor: Sensor

    override fun onCreateEngine(): WallpaperService.Engine {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        val engine = LiveWallpaperEngine()
        return engine
    }

    private inner class LiveWallpaperEngine : GLWallpaperService.GLEngine(), SensorEventListener {
        private var renderer: Live2DRenderer?

        init {
            renderer = Live2DRenderer(applicationContext)
            setRenderer(renderer)
            renderMode = GLWallpaperService.GLEngine.RENDERMODE_CONTINUOUSLY
        }

        override fun onResume() {
            super.onResume()
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
        }

        override fun onPause() {
            super.onPause()
            sensorManager.unregisterListener(this)
        }

        override fun onSensorChanged(event: SensorEvent) {

        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {

        }

        override fun onDestroy() {
            super.onDestroy()
            if (renderer != null) {
                renderer!!.release()
                renderer = null
            }
        }
    }
}
