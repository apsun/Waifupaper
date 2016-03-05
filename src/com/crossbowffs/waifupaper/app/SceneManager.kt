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
import com.crossbowffs.waifupaper.utils.useNotNull

class SceneManager(private val context: Context, private val engine: GLEngine2) :
        SharedPreferences.OnSharedPreferenceChangeListener,
        SensorEventListener {
    private lateinit var preferences: SharedPreferences
    private lateinit var sensorManager: SensorManager
    private lateinit var sensor: Sensor

    private lateinit var transformManager: SceneTransformManager
    private lateinit var renderer: SceneRenderer

    private var enableTilt: Boolean = true
    private var enableTouch: Boolean = true
    private var enableSound: Boolean = true
    private var enableWallpaperParallax: Boolean = true

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
        updateSelectedModel()
        updateSelectedBackground()
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
            PrefConsts.PREF_MODEL_NAME -> updateSelectedModel()
            PrefConsts.PREF_ENABLE_MODEL_TILT -> null
            PrefConsts.PREF_ENABLE_MODEL_TOUCH -> null
            PrefConsts.PREF_ENABLE_SOUND -> null
            PrefConsts.PREF_BACKGROUND_FILE_NAME -> updateSelectedBackground()
            PrefConsts.PREF_ENABLE_BACKGROUND_PARALLAX -> null
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {

    }

    fun updateSelectedModel() {
        val data = preferences.getString(PrefConsts.PREF_MODEL_NAME, null)?.split(':')
        val name = data?.get(0) ?: "Epsilon"
        val location = data?.get(1).useNotNull { FileLocation.valueOf(it) } ?: FileLocation.INTERNAL
        (object : AsyncTask<Unit, Unit, Unit>() {
            override fun doInBackground(vararg params: Unit?) {
                val newModelInfo = AssetLoader.loadModelInfo(context, name, location)
                val newModelData = AssetLoader.loadModel(context, newModelInfo)
                val soundPool = AssetLoader.loadSounds(context, newModelInfo)
                engine.queueEvent(Runnable {
                    renderer.setModel(newModelData)
                    renderer.setSoundPool(soundPool)
                })
            }
        }).execute()
    }

    fun updateSelectedBackground() {
        // TODO
        val name = "back_class_normal.png"
        val location = FileLocation.INTERNAL
        (object : AsyncTask<Unit, Unit, Unit>() {
            override fun doInBackground(vararg params: Unit?) {
                val background = AssetLoader.loadBackground(context, name, location)
                engine.queueEvent(Runnable {
                    renderer.setBackground(background)
                })
            }
        }).execute()
    }
}
