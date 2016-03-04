package com.crossbowffs.waifupaper.app

import android.content.Context
import com.crossbowffs.waifupaper.loader.*
import com.crossbowffs.waifupaper.utils.useNotNull
import jp.live2d.android.Live2DModelAndroid
import jp.live2d.android.UtOpenGL
import jp.live2d.framework.*
import jp.live2d.motion.Live2DMotion
import jp.live2d.motion.MotionQueueManager
import jp.live2d.utils.android.SimpleImage
import net.rbgrn.android.glwallpaperservice.GLWallpaperService
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class Live2DRenderer(private var context: Context) : GLWallpaperService.Renderer {
    private val ENABLE_SOUNDS = true
    private var canvasWidth: Float = 0f
    private var canvasHeight: Float = 0f

    private var gl: GL10? = null
    private val motionManager: MotionQueueManager
    private var modelWrapper: Live2DModelWrapper? = null
    private var soundPool: SoundPoolWrapper? = null
    private var motionGroupIndex: Int = -1
    private var motionIndex: Int = -1
    private var bg: SimpleImage? = null
    private var bgModelPosition: BGModelPosition? = null

    init {
        motionManager = MotionQueueManager()
    }

    private val hasModel: Boolean
        get() = modelWrapper != null

    private val model: Live2DModelAndroid
        get() = modelWrapper!!.model

    private val physics: L2DPhysics?
        get() = modelWrapper!!.physics

    private val pose: L2DPose?
        get() = modelWrapper!!.pose

    private val expressions: Array<Live2DExpressionWrapper>?
        get() = modelWrapper!!.expressions

    private val layoutMatrix: L2DModelMatrix
        get() = modelWrapper!!.layoutMatrix

    private val motionGroups: Array<Live2DMotionGroupWrapper>?
        get() = modelWrapper!!.motionGroups

    override fun onDrawFrame(gl: GL10) {
        gl.glMatrixMode(GL10.GL_MODELVIEW)
        gl.glLoadIdentity()
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT)
        gl.glEnable(GL10.GL_BLEND)
        gl.glBlendFunc(GL10.GL_ONE, GL10.GL_ONE_MINUS_SRC_ALPHA)
        gl.glDisable(GL10.GL_DEPTH_TEST)
        gl.glDisable(GL10.GL_CULL_FACE)

        bg?.draw(gl)

        if (!hasModel) {
            return
        }

        model.loadParam()

        if (motionGroups != null) {
            if (motionManager.isFinished) {
                motionManager.startMotion(chooseMotion(), false)
            } else {
                motionManager.updateParam(model)
            }
        }

        model.saveParam()

        bgModelPosition?.update(System.nanoTime())
        val modelXY = bgModelPosition?.getModelXY()
        val dragX = modelXY?.first ?: 0f
        val dragY = modelXY?.second ?: 0f
        model.addToParamFloat(L2DStandardID.PARAM_ANGLE_X, dragX * 30f)
        model.addToParamFloat(L2DStandardID.PARAM_ANGLE_Y, dragY * 30f)
        model.addToParamFloat(L2DStandardID.PARAM_BODY_ANGLE_X, dragX * 10f)

        if (bgModelPosition != null) {
            val bgShiftXY = bgModelPosition!!.getBGShiftXY()
            val bgMaxShiftXY =
                    bgModelPosition!!.bgMaxShiftX to bgModelPosition!!.bgMaxShiftY
            bg?.setDrawRect(
                    (bgShiftXY.first - bgMaxShiftXY.first) * canvasWidth,
                    (1f + bgShiftXY.first + bgMaxShiftXY.first) * canvasWidth,
                    (1f + bgShiftXY.second + bgMaxShiftXY.second) * canvasHeight,
                    (bgShiftXY.second - bgMaxShiftXY.second) * canvasHeight)
        }

        physics?.updateParam(model)
        pose?.updateParam(model)

        model.update()
        // TODO: For some reason this doesn't work
        // FIXME: The background flashes black sometimes after loading a pose
        // gl.glPushMatrix()
        // gl.glMultMatrixf(layoutMatrix.array, 0)
        model.draw()
        // gl.glPopMatrix()
    }

    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        gl.glViewport(0, 0, width, height)
        gl.glMatrixMode(GL10.GL_PROJECTION)
        gl.glLoadIdentity()
        if (!hasModel) return
        // TODO This projection matrix may be too large, exposing
        // TODO     chopped off limbs where the model canvas ends
        canvasWidth = model.canvasWidth
        canvasHeight = model.canvasWidth * height / width
        gl.glOrthof(
            0f,
            canvasWidth,
            canvasHeight,
            0f,
            0.5f,
            -0.5f)
    }

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        this.gl = gl
        if (modelWrapper != null) {
            loadGLTextures(modelWrapper!!)
        }
        val width: Int = context.resources.displayMetrics.widthPixels
        val height: Int = context.resources.displayMetrics.heightPixels
        canvasWidth = model.canvasWidth
        canvasHeight = model.canvasWidth * height / width
        loadBackground()
    }

    fun onResume() {

    }

    fun onPause() {
        soundPool?.stopSound()
    }

    fun playSoundForMotion() {
        if (soundPool == null) return
        motionGroups!![motionGroupIndex].motions[motionIndex].soundFilePath.useNotNull {
            soundPool!!.playSound(it)
        }
    }

    fun chooseMotion(): Live2DMotion {
        var currMotion = motionGroups!![motionGroupIndex]
        if (++motionIndex == currMotion.motions.size) {
            motionIndex = 0
            motionGroupIndex = (motionGroupIndex + 1) % motionGroups!!.size
            currMotion = motionGroups!![motionGroupIndex]
        }
        playSoundForMotion()
        return currMotion.motions[motionIndex].motion
    }

    fun loadGLTextures(modelData: Live2DModelWrapper) {
        if (gl == null) return // Textures will be loaded in onSurfaceCreated
        modelData.model.setGL(gl)
        for (i in modelData.textures.indices) {
            val textureNum = UtOpenGL.buildMipmap(gl, modelData.textures[i], false)
            modelData.model.setTexture(i, textureNum)
        }
    }

    fun setModel(name: String, location: FileLocation) {
        release()
        val newModelInfo = Live2DModelLoader.loadInfo(context, name, location)
        val newModelData = Live2DModelLoader.loadModel(context, newModelInfo)
        if (ENABLE_SOUNDS) {
            soundPool = Live2DModelLoader.loadSounds(context, newModelInfo)
        }
        loadGLTextures(newModelData)
        modelWrapper = newModelData
        motionGroupIndex = 0
        motionIndex = 0
    }

    fun setPositioner(positioner: BGModelPosition) {
        this.bgModelPosition = positioner
    }

    fun release() {
        motionGroupIndex = -1
        motionIndex = -1
        motionManager.stopAllMotions()
        soundPool?.release()
        soundPool = null
        if (modelWrapper != null) {
            val model = modelWrapper!!.model
            model.deleteTextures()
            model.setGL(null)
            modelWrapper = null
        }
    }

    private fun loadBackground() {
        //TODO: Custom background loading
        bg = Live2DModelLoader.loadBackground(context, gl, "testBG.png")
        bg!!.setDrawRect(0f, canvasWidth, canvasHeight, 0f)
        //TODO Read from preferences to determine area to crop
        bg!!.setUVRect(0.0f, 1.0f, 0.0f, 1.0f)
    }
}
