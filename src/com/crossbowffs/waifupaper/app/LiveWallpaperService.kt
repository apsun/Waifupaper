package com.crossbowffs.waifupaper.app

import android.view.MotionEvent
import android.view.SurfaceHolder
import net.rbgrn.android.glwallpaperservice.GLWallpaperService

class LiveWallpaperService : GLWallpaperService() {
    override fun onCreateEngine(): GLEngine = LiveWallpaperEngine()

    private inner class LiveWallpaperEngine : GLWallpaperService.GLEngine(), GLEngine2 {
        private val sceneManager: SceneManager

        init {
            sceneManager = SceneManager(this@LiveWallpaperService, this)
        }

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            sceneManager.onCreate()
        }

        override fun onResume() {
            super.onResume()
            sceneManager.onResume()
        }

        override fun onPause() {
            super.onPause()
            sceneManager.onPause()
        }

        override fun onDestroy() {
            super.onDestroy()
            sceneManager.onDestroy()
        }

        override fun onTouchEvent(event: MotionEvent) {
            sceneManager.onTouchEvent(event)
        }
    }
}
