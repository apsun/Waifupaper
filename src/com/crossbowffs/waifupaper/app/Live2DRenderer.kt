package com.crossbowffs.waifupaper.app

import android.content.Context
import com.crossbowffs.waifupaper.loader.*
import com.crossbowffs.waifupaper.utils.useNotNull
import jp.live2d.android.Live2DModelAndroid
import jp.live2d.android.UtOpenGL
import jp.live2d.framework.*
import jp.live2d.motion.Live2DMotion
import jp.live2d.motion.MotionQueueManager
import net.rbgrn.android.glwallpaperservice.GLWallpaperService
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class Live2DRenderer(private var context: Context) : GLWallpaperService.Renderer {
    private val ENABLE_SOUNDS = true

    private val motionMgr: MotionQueueManager
    private val dragMgr: L2DTargetPoint
    private var modelWrapper: Live2DModelWrapper? = null
    private var soundPool: SoundPoolWrapper? = null
    private var motionIndex: Int = -1
    private var subMotionIndex: Int = -1

    init {
        dragMgr = L2DTargetPoint()
        motionMgr = MotionQueueManager()
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

    private val motions: Array<Live2DMotionGroupWrapper>?
        get() = modelWrapper!!.motionGroups

    fun playSoundForMotion() {
        if (soundPool == null) return
        motions!![motionIndex].motions[subMotionIndex].soundFilePath.useNotNull {
            soundPool!!.playSound(it)
        }
    }

    fun chooseMotion(): Live2DMotion {
        var currMotion = motions!![motionIndex]
        if (++subMotionIndex == currMotion.motions.size) {
            subMotionIndex = 0
            motionIndex = (motionIndex + 1) % currMotion.motions.size
            currMotion = motions!![motionIndex]
        }
        playSoundForMotion()
        return currMotion.motions[subMotionIndex].motion
    }

    override fun onDrawFrame(gl: GL10) {
        gl.glMatrixMode(GL10.GL_MODELVIEW)
        gl.glLoadIdentity()
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT)
        gl.glEnable(GL10.GL_BLEND)
        gl.glBlendFunc(GL10.GL_ONE, GL10.GL_ONE_MINUS_SRC_ALPHA)
        gl.glDisable(GL10.GL_DEPTH_TEST)
        gl.glDisable(GL10.GL_CULL_FACE)

        if (!hasModel) {
            return
        }

        model.loadParam()

        if (motions != null) {
            if (motionMgr.isFinished) {
                motionMgr.startMotion(chooseMotion(), false)
            } else {
                motionMgr.updateParam(model)
            }
        }

        model.saveParam()

        dragMgr.update()
        val dragX = dragMgr.x
        val dragY = dragMgr.y
        model.addToParamFloat(L2DStandardID.PARAM_ANGLE_X, dragX * 15)
        model.addToParamFloat(L2DStandardID.PARAM_ANGLE_Y, dragY * 15)
        model.addToParamFloat(L2DStandardID.PARAM_BODY_ANGLE_X, dragX * 30)

        physics?.updateParam(model)
        pose?.updateParam(model)

        model.update()
        // TODO: For some reason this doesn't work
        // gl.glPushMatrix()
        // gl.glMultMatrixf(layoutMatrix.array, 0)
        model.draw()
        // gl.glPopMatrix()
    }

    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        gl.glViewport(0, 0, width, height)
        gl.glMatrixMode(GL10.GL_PROJECTION)
        gl.glLoadIdentity()
        val modelWidth = model.canvasWidth
        gl.glOrthof(
            0f,
            modelWidth,
            modelWidth * height / width,
            0f,
            0.5f,
            -0.5f)
    }

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        if (modelWrapper == null) {
            setModel(gl, "shizuku")
        } else {
            loadGLTextures(gl, modelWrapper!!)
        }
    }

    fun loadGLTextures(gl: GL10, modelData: Live2DModelWrapper) {
        modelData.model.setGL(gl)
        for (i in modelData.textures.indices) {
            val textureNum = UtOpenGL.buildMipmap(gl, modelData.textures[i], false)
            modelData.model.setTexture(i, textureNum)
        }
    }

    fun setModel(gl: GL10, name: String) {
        release()
        val newModelInfo = Live2DModelLoader.loadInternalInfo(context, name)
        val newModelData = Live2DModelLoader.loadInternalModel(context, newModelInfo)
        if (ENABLE_SOUNDS) {
            soundPool = Live2DModelLoader.loadInternalSounds(context, newModelInfo)
        }
        loadGLTextures(gl, newModelData)
        modelWrapper = newModelData
        motionIndex = 0
        subMotionIndex = 0
    }

    fun release() {
        motionIndex = -1
        subMotionIndex = -1
        motionMgr.stopAllMotions()
        if (modelWrapper != null) {
            val model = modelWrapper!!.model
            model.deleteTextures()
            model.setGL(null)
            soundPool?.release()
            modelWrapper = null
        }
    }

    fun resetDrag() {
        dragMgr.set(0f, 0f)
    }


    fun drag(x: Float, y: Float) {
        dragMgr.set(x, y)
    }
}
