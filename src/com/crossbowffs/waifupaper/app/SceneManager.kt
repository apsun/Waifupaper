package com.crossbowffs.waifupaper.app

import android.content.Context
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.AsyncTask
import android.preference.PreferenceManager
import android.view.MotionEvent
import com.crossbowffs.waifupaper.loader.AssetLoader
import com.crossbowffs.waifupaper.loader.FileLocation
import com.crossbowffs.waifupaper.rendering.SceneRenderer
import com.crossbowffs.waifupaper.rendering.SceneTransformManager
import com.crossbowffs.waifupaper.utils.loge

class SceneManager(private val context: Context, private val engine: GLEngine2) :
        SharedPreferences.OnSharedPreferenceChangeListener,
        SensorEventListener {
    private lateinit var preferences: SharedPreferences
    private lateinit var sensorManager: SensorManager
    private lateinit var sensor: Sensor

    private lateinit var transformManager: SceneTransformManager
    private lateinit var renderer: SceneRenderer

    private var enableTilt: Boolean = false
    private var enableTouch: Boolean = false
    private var enableSound: Boolean = false
    private var enableWallpaperParallax: Boolean = false

    fun onCreate() {
        preferences = PreferenceManager.getDefaultSharedPreferences(context)
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        transformManager = SceneTransformManager(0.5f, 0.5f, 0.14f, 0.07f)
        renderer = SceneRenderer(context)
        renderer.setTransformManager(transformManager)
        engine.setRenderer(renderer)

        // Must be called AFTER setRenderer, since the async GL runner queue
        // is initialized after that call
        preferences.registerOnSharedPreferenceChangeListener(this)
    }

    fun initialize() {
        setSelectedModedlFromPrefs()
    }

    fun onResume() {
        if (enableTilt || enableWallpaperParallax) {
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
        }
        renderer.onResume()
    }

    fun onPause() {
        sensorManager.unregisterListener(this) // TODO: Do we need checks, or are errors ignored?
        renderer.onPause()
    }

    fun onDestroy() {
        renderer.release()
        preferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        val width = renderer.surfaceWidth
        val height = renderer.surfaceHeight
        if (width == null || height == null) {
            loge("onTouchEvent called before surface size obtained")
            return false
        }

        when (event.action) {
            MotionEvent.ACTION_DOWN -> transformManager.touchDown(event.x / width, event.y / height)
            MotionEvent.ACTION_MOVE -> transformManager.touchChanged(event.x / width, event.y / height)
            MotionEvent.ACTION_UP -> transformManager.touchUp()
            else -> return false
        }

        return true
    }

    override fun onSensorChanged(event: SensorEvent) {
        transformManager.gyroChanged(event.values[0], event.values[1], event.values[2], event.timestamp)
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String) {
        // TODO
        when (key) {
            PrefConsts.PREF_MODEL_NAME -> setSelectedModedlFromPrefs()
            PrefConsts.PREF_ENABLE_MODEL_TILT -> null
            PrefConsts.PREF_ENABLE_MODEL_TOUCH -> null
            PrefConsts.PREF_ENABLE_SOUND -> null
            PrefConsts.PREF_BACKGROUND_FILE_NAME -> null
            PrefConsts.PREF_ENABLE_BACKGROUND_PARALLAX -> null
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {

    }

    fun setSelectedModedlFromPrefs() {
        val s = getSelectedModel() ?: "Epsilon" to FileLocation.INTERNAL
        setModel(s.first, s.second)
    }

    private fun getSelectedModel(): Pair<String, FileLocation>? {
        val data = preferences.getString(PrefConsts.PREF_MODEL_NAME, null)?.split(':')
        if (data != null) {
            return data[0] to FileLocation.valueOf(data[1])
        }
        return null
    }

    private fun setModel(name: String, location: FileLocation) {
        (object : AsyncTask<Unit, Unit, Unit>() {
            override fun doInBackground(vararg p0: Unit?) {
                val newModelInfo = AssetLoader.loadModelInfo(context, name, location)
                val newModelData = AssetLoader.loadModel(context, newModelInfo)
                val soundPool = AssetLoader.loadSounds(context, newModelInfo)
                val backgrond = AssetLoader.loadBackground(context, "back_class_normal.png", FileLocation.INTERNAL)
                engine.queueEvent(Runnable {
                    renderer.setModel(newModelData)
                    renderer.setSoundPool(soundPool)
                    renderer.setBackground(backgrond)
                })
            }
        }).execute()
    }
}
