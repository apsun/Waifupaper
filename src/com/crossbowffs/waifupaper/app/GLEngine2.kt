package com.crossbowffs.waifupaper.app

import net.rbgrn.android.glwallpaperservice.GLWallpaperService

/**
 * Encapsulates common funtionality between LiveWallpaperService and
 * SceneGLSurfaceView.
 */
interface GLEngine2 {
    fun queueEvent(runnable: Runnable)
    fun requestRender()
    fun setRenderer(renderer: GLWallpaperService.Renderer)
}
