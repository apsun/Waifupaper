/**

 * You can modify and use this source freely
 * only for the development of application related Live2D.

 * (c) Live2D Inc. All rights reserved.
 */
package com.crossbowffs.waifupaper


import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.MotionEvent
import net.rbgrn.android.glwallpaperservice.GLWallpaperService

class LiveWallpaperService : GLWallpaperService() {

    private var sensorManager: SensorManager? = null

    override fun onCreateEngine(): WallpaperService.Engine {

        val engine = MyEngine()

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorManager!!.registerListener(engine, sensorManager!!.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_GAME)
        return engine

    }

    internal inner class MyEngine : GLWallpaperService.GLEngine(), SensorEventListener {
        var renderer: Live2DRenderer? = null
        var ax: Float = 0.toFloat()
        var ay: Float = 0.toFloat()
        var az: Float = 0.toFloat()   // these are the acceleration in x,y and z axis

        init {
            // handle prefs, other initialization
            renderer = Live2DRenderer(applicationContext)
            setRenderer(renderer)
            renderMode = GLWallpaperService.GLEngine.RENDERMODE_CONTINUOUSLY
        }


        override fun onAccuracyChanged(arg0: Sensor, arg1: Int) {
        }

        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {

                ax = event.values[0]
                ay = event.values[1]
                az = event.values[2]
                val len = Math.sqrt((ax * ax + ay * ay * az * az).toDouble()).toFloat()
                ax *= 1 / len
                ay *= 1 / len
                az *= 1 / len
                renderer!!.drag(ax, 0f)

                Log.d("ASDF", String.format("%f,%f,%f", ax, ay, az))
            }
        }

        override fun onTouchEvent(event: MotionEvent) {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                }
                MotionEvent.ACTION_UP -> {
                }
                MotionEvent.ACTION_MOVE -> {
                }
                MotionEvent.ACTION_CANCEL -> {
                }
            }//renderer.resetDrag();
            //renderer.drag(event.getX(), event.getY());
        }

        override fun onDestroy() {
            super.onDestroy()
            if (renderer != null) {
                renderer!!.release()
            }
            renderer = null
        }
    }
}
