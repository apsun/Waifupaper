package com.crossbowffs.waifupaper.rendering

import android.content.Context
import android.graphics.Bitmap
import com.crossbowffs.waifupaper.loader.Live2DExpressionWrapper
import com.crossbowffs.waifupaper.loader.Live2DModelWrapper
import com.crossbowffs.waifupaper.loader.SoundPoolWrapper
import com.crossbowffs.waifupaper.utils.useNotNull
import jp.live2d.android.Live2DModelAndroid
import jp.live2d.android.UtOpenGL
import jp.live2d.framework.L2DModelMatrix
import jp.live2d.framework.L2DPhysics
import jp.live2d.framework.L2DPose
import jp.live2d.framework.L2DStandardID
import net.rbgrn.android.glwallpaperservice.GLWallpaperService
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class SceneRenderer(private var context: Context) : GLWallpaperService.Renderer {
    private var heightRatio: Float = 1f

    private var gl: GL10? = null

    private var motionManager: MotionManager? = null
    private var transformManager: SceneTransformManager? = null

    // Resources
    private var modelWrapper: Live2DModelWrapper? = null
    private var soundPool: SoundPoolWrapper? = null
    private var background: GLBitmap? = null

    private val model: Live2DModelAndroid?
        get() = modelWrapper?.model

    private val physics: L2DPhysics?
        get() = modelWrapper?.physics

    private val pose: L2DPose?
        get() = modelWrapper?.pose

    private val expressions: Array<Live2DExpressionWrapper>? // TODO: How do we use this?
        get() = modelWrapper?.expressions

    private val layoutMatrix: L2DModelMatrix?
        get() = modelWrapper?.layoutMatrix


    override fun onDrawFrame(gl: GL10) {
        gl.glMatrixMode(GL10.GL_MODELVIEW)
        gl.glLoadIdentity()
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT)
        gl.glEnable(GL10.GL_BLEND)
        gl.glBlendFunc(GL10.GL_ONE, GL10.GL_ONE_MINUS_SRC_ALPHA)
        gl.glDisable(GL10.GL_DEPTH_TEST)
        gl.glDisable(GL10.GL_CULL_FACE)

        if (background != null && transformManager != null) {
            val bgShiftXY = transformManager!!.getBGShiftXY()
            val bgMaxShiftX = transformManager!!.bgMaxShiftX
            val bgMaxShiftY = transformManager!!.bgMaxShiftY
            background!!.setBounds(
                    bgShiftXY.x - bgMaxShiftX,
                    1f + bgShiftXY.x + bgMaxShiftX,
                    heightRatio + bgShiftXY.y + bgMaxShiftY,
                    bgShiftXY.y - bgMaxShiftY
            )
        }

        background?.draw(gl)

        val model = this.model ?: return
        //TODO It'll just show the BG if there's no model. Should I explicitly make it a black screen?

        gl.glScalef(1f / model.canvasWidth, 1f / model.canvasWidth, 1f / model.canvasWidth)

        motionManager?.update()

        transformManager?.update(System.nanoTime())
        val modelXY = transformManager?.getModelXY()
        val dragX = modelXY?.x ?: 0f
        val dragY = modelXY?.y ?: 0f
        model.addToParamFloat(L2DStandardID.PARAM_ANGLE_X, dragX * 30f)
        model.addToParamFloat(L2DStandardID.PARAM_ANGLE_Y, dragY * 30f)
        model.addToParamFloat(L2DStandardID.PARAM_BODY_ANGLE_X, dragX * 10f)

        physics?.updateParam(model)
        pose?.updateParam(model)

        model.update()
        // FIXME: The background flashes black sometimes after loading a pose
        // TODO: For some reason the following doesn't work
        // Probably is a coordinate system problem
        // gl.glPushMatrix()
        // gl.glMultMatrixf(layoutMatrix.array, 0)
        model.draw()
        // gl.glPopMatrix()
    }

    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        gl.glViewport(0, 0, width, height)
        gl.glMatrixMode(GL10.GL_PROJECTION)
        gl.glLoadIdentity()
        heightRatio = height.toFloat() / width.toFloat()
        gl.glOrthof(
            0f,
            1f,
            heightRatio,
            0f,
            0.5f,
            -0.5f)
    }

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        this.gl = gl
        genModelTextures()
        genBackgroundTextures()
    }

    fun onResume() {

    }

    fun onPause() {
        soundPool?.stopSound()
        motionManager?.stopMotion()
    }

    fun genModelTextures() {
        val gl = this.gl ?: return
        val modelWrapper = this.modelWrapper
        if (modelWrapper != null) {
            modelWrapper.model.setGL(gl)
            for (i in modelWrapper.textures.indices) {
                val textureNum = UtOpenGL.buildMipmap(gl, modelWrapper.textures[i], false)
                modelWrapper.model.setTexture(i, textureNum)
            }
        }
    }

    fun genBackgroundTextures() {
        val background = this.background ?: return
        background.setGL(gl)
        background.setBounds(0f, 1f, heightRatio, 0f)
    }

    fun releaseModel() {
        motionManager?.stopMotion()
        motionManager = null

        val oldModelWrapper = modelWrapper
        if (oldModelWrapper != null) {
            val model = oldModelWrapper.model
            model.deleteTextures()
            model.setGL(null)
            oldModelWrapper.textures.forEach { it.recycle() }
            modelWrapper = null
        }
    }

    fun releaseSoundPool() {
        val oldSoundPool = soundPool
        if (oldSoundPool != null) {
            oldSoundPool.stopSound()
            oldSoundPool.release()
            soundPool = null
        }
    }

    fun releaseBackground() {
        val oldBackground = background
        if (oldBackground != null) {
            oldBackground.recycle()
            background = null
        }
    }

    fun release() {
        releaseModel()
        releaseSoundPool()
        releaseBackground()
    }

    fun setModel(newModelWrapper: Live2DModelWrapper?) {
        val oldModelWrapper = modelWrapper
        if (oldModelWrapper == newModelWrapper) return
        releaseModel()
        modelWrapper = newModelWrapper
        genModelTextures()
        motionManager = MotionManager.of(newModelWrapper) {
            soundPool?.playSound(it)
        }
    }

    fun setSoundPool(newSoundPool: SoundPoolWrapper?) {
        val oldSoundPool = soundPool
        if (oldSoundPool == newSoundPool) return
        releaseSoundPool()
        soundPool = newSoundPool
    }

    fun setBackground(newBackground: Bitmap?) {
        val oldBackground = background
        if (oldBackground?.bitmap == newBackground) return
        releaseBackground()
        background = newBackground.useNotNull { GLBitmap(it) }
        genBackgroundTextures()
    }

    fun setTransformManager(transformManager: SceneTransformManager) {
        this.transformManager = transformManager
    }
}
