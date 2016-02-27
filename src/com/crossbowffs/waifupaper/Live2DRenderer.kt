/**

 * You can modify and use this source freely
 * only for the development of application related Live2D.

 * (c) Live2D Inc. All rights reserved.
 */
package com.crossbowffs.waifupaper

import android.content.Context
import android.content.res.AssetManager
import jp.live2d.android.Live2DModelAndroid
import jp.live2d.android.UtOpenGL
import jp.live2d.framework.L2DExpressionMotion
import jp.live2d.framework.L2DPhysics
import jp.live2d.framework.L2DStandardID
import jp.live2d.framework.L2DTargetPoint
import jp.live2d.motion.Live2DMotion
import jp.live2d.motion.MotionQueueManager
import net.rbgrn.android.glwallpaperservice.GLWallpaperService

import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import java.io.IOException
import java.io.InputStream

class Live2DRenderer(internal var con: Context) : GLWallpaperService.Renderer {

    private var live2DModel: Live2DModelAndroid? = null
    private var motion: Live2DMotion? = null
    private var motionMgr: MotionQueueManager? = null
    private var dragMgr: L2DTargetPoint? = null
    private var physics: L2DPhysics? = null

    internal val MODEL_PATH = "epsilon/Epsilon.moc"
    internal val TEXTURE_PATHS = arrayOf("epsilon/Epsilon.1024/texture_00.png", "epsilon/Epsilon.1024/texture_01.png", "epsilon/Epsilon.1024/texture_02.png")
    internal val MOTION_PATH = "epsilon/motions/Epsilon_idle_01.mtn"
    internal val PHYSICS_PATH = "epsilon/Epsilon.physics.json"

    internal var glWidth = 0f
    internal var glHeight = 0f


    init {
        dragMgr = L2DTargetPoint()
        motionMgr = MotionQueueManager()
    }


    override fun onDrawFrame(gl: GL10) {
        // Your rendering code goes here
        gl.glMatrixMode(GL10.GL_MODELVIEW)
        gl.glLoadIdentity()
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT)
        gl.glEnable(GL10.GL_BLEND)
        gl.glBlendFunc(GL10.GL_ONE, GL10.GL_ONE_MINUS_SRC_ALPHA)
        gl.glDisable(GL10.GL_DEPTH_TEST)
        gl.glDisable(GL10.GL_CULL_FACE)


        live2DModel.loadParam()

        if (motionMgr.isFinished) {
            motionMgr.startMotion(motion, false)
        } else {
            motionMgr.updateParam(live2DModel)
        }

        live2DModel.saveParam()

        dragMgr.update()

        val dragX = dragMgr.x
        val dragY = dragMgr.y
        live2DModel.addToParamFloat(L2DStandardID.PARAM_ANGLE_X, dragX * 15)
        live2DModel.addToParamFloat(L2DStandardID.PARAM_ANGLE_Y, dragY * 15)
        live2DModel.addToParamFloat(L2DStandardID.PARAM_BODY_ANGLE_X, dragX * 30)

        physics.updateParam(live2DModel)

        live2DModel.setGL(gl)

        live2DModel.update()
        live2DModel.draw()

    }

    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {

        gl.glViewport(0, 0, width, height)


        gl.glMatrixMode(GL10.GL_PROJECTION)
        gl.glLoadIdentity()

        val modelWidth = live2DModel.canvasWidth

        gl.glOrthof(
                0f,
                modelWidth,
                modelWidth * height / width,
                0f,
                0.5f, -0.5f)

        glWidth = width.toFloat()
        glHeight = height.toFloat()
    }

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        val mngr = con.assets
        try {
            val `in` = mngr.open(MODEL_PATH)
            live2DModel = Live2DModelAndroid.loadModel(`in`)
            `in`.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        try {
            //texture
            for (i in TEXTURE_PATHS.indices) {
                val `in` = mngr.open(TEXTURE_PATHS[i])
                val texNo = UtOpenGL.loadTexture(gl, `in`, true)
                live2DModel.setTexture(i, texNo)
                `in`.close()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        try {
            var `in` = mngr.open(MOTION_PATH)
            motion = Live2DMotion.loadMotion(`in`)
            `in`.close()

            `in` = mngr.open(PHYSICS_PATH)
            physics = L2DPhysics.load(`in`)
            `in`.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    /**
     * Called when the engine is destroyed. Do any necessary clean up because
     * at this point your renderer instance is now done for.
     */
    fun release() {
    }

    fun resetDrag() {
        dragMgr.set(0f, 0f)
    }


    fun drag(x: Float, y: Float) {
        dragMgr.set(x, y)
    }
}
