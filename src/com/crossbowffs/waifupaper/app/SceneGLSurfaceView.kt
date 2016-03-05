package com.crossbowffs.waifupaper.app

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import net.rbgrn.android.glwallpaperservice.GLWallpaperService

class SceneGLSurfaceView(context: Context, attrs: AttributeSet) : GLSurfaceView(context, attrs), GLEngine2 {
    private lateinit var sceneManager: SceneManager

    override fun setRenderer(renderer: GLWallpaperService.Renderer) {
        setRenderer(renderer as Renderer)
    }

    init {
        sceneManager = SceneManager(context, this)
        sceneManager.onCreate()
        setOnTouchListener{ view, motionEvent -> sceneManager.onTouchEvent(motionEvent) }
    }

    override fun onResume() {
        super.onResume()
        sceneManager.onResume()
    }

    override fun onPause() {
        super.onPause()
        sceneManager.onPause()
    }

    override fun onDetachedFromWindow() {
        // TODO: Maybe this should go in the finalizer instead
        super.onDetachedFromWindow()
        sceneManager.onDestroy()
    }
}
