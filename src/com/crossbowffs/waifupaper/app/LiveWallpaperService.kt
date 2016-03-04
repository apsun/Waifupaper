package com.crossbowffs.waifupaper.app

import android.content.Context
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import android.preference.PreferenceManager
import android.view.MotionEvent
import android.view.SurfaceHolder
import com.crossbowffs.waifupaper.loader.FileLocation
import com.crossbowffs.waifupaper.utils.useNotNull
import net.rbgrn.android.glwallpaperservice.GLWallpaperService

class LiveWallpaperService : GLWallpaperService() {
    private lateinit var preferences: SharedPreferences
    private lateinit var sensorManager: SensorManager
    private lateinit var sensor: Sensor

    override fun onCreate() {
        super.onCreate()
        preferences = PreferenceManager.getDefaultSharedPreferences(this)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    }

    private fun getSelectedModel(): Pair<String, FileLocation>? {
        val data = preferences.getString("selectedModel", null)?.split(':')
        if (data != null) {
            return data[0] to FileLocation.valueOf(data[1])
        }
        return null
    }

    override fun onCreateEngine(): GLEngine = LiveWallpaperEngine()

    private inner class LiveWallpaperEngine : GLWallpaperService.GLEngine(),
            SharedPreferences.OnSharedPreferenceChangeListener {
        private lateinit var renderer: Live2DRenderer
        private lateinit var screenDimensions: Pair<Int, Int>
        private lateinit var bgModelPosition: BGModelPosition
        private lateinit var sensorListener: SensorEventListener
        private lateinit var sensorThread: HandlerThread
        private lateinit var sensorHandler: Handler

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            renderer = Live2DRenderer(applicationContext)
            getSelectedModel().useNotNull { renderer.setModel(it.first, it.second) }
            setRenderer(renderer)
            renderMode = GLWallpaperService.GLEngine.RENDERMODE_CONTINUOUSLY
            preferences.registerOnSharedPreferenceChangeListener(this)
            val displayMetrics = applicationContext.resources.displayMetrics
            screenDimensions = Pair(displayMetrics.widthPixels, displayMetrics.heightPixels)
            this.setTouchEventsEnabled(true)

            bgModelPosition = BGModelPosition(0.5f, 0.5f, 0.14f, 0.07f)
            renderer.setPositioner(bgModelPosition)
            sensorListener = object: SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    bgModelPosition.gyroChanged(event.values[0], event.values[1],
                            event.values[2], event.timestamp)
                }
                override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
            }
            sensorThread = HandlerThread("Sensor Thread", Thread.MAX_PRIORITY)
            sensorThread.start()
            sensorHandler = Handler(sensorThread.looper)
        }

        override fun onResume() {
            super.onResume()
            sensorManager.registerListener(
                    sensorListener, sensor, SensorManager.SENSOR_DELAY_GAME, sensorHandler)
            renderer.onResume()
        }

        override fun onPause() {
            super.onPause()
            sensorManager.unregisterListener(sensorListener)
            renderer.onPause()
        }

        override fun onDestroy() {
            super.onDestroy()
            renderer.release()
            sensorThread.quitSafely()
        }

        override fun onTouchEvent(event: MotionEvent) {
            when (event.action) {
                MotionEvent.ACTION_DOWN ->
                    bgModelPosition.touchDown(event.x / screenDimensions.first,
                            event.y / screenDimensions.second)
                MotionEvent.ACTION_MOVE ->
                    bgModelPosition.touchChanged(event.x / screenDimensions.first,
                            event.y / screenDimensions.second)
                MotionEvent.ACTION_UP -> bgModelPosition.touchUp()
            }
        }

        override fun onSharedPreferenceChanged(p0: SharedPreferences?, p1: String?) {
            if (p1 == "selectedModel") {
                getSelectedModel().useNotNull { renderer.setModel(it.first, it.second) }
            }
        }
    }
}
