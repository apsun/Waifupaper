package com.crossbowffs.waifupaper.loader

import android.graphics.Bitmap
import jp.live2d.android.Live2DModelAndroid
import jp.live2d.framework.L2DExpressionMotion
import jp.live2d.framework.L2DModelMatrix
import jp.live2d.framework.L2DPhysics
import jp.live2d.framework.L2DPose
import jp.live2d.motion.Live2DMotion

class Live2DMotionWrapper(
    val motion: Live2DMotion,
    val soundId: Int?,
    val fadeInDuration: Int?,
    val fadeOutDuration: Int?
)

class Live2DMotionGroupWrapper(
    val name: String,
    val motions: Array<Live2DMotionWrapper>
)

class Live2DExpressionWrapper(
    val name: String,
    val expression: L2DExpressionMotion
)

/**
 * This class holds data for Live2D models that does not depend
 * on an OpenGL context. You must close this object when you are
 * done using it.
 */
class Live2DModelWrapper(
    val name: String,
    val model: Live2DModelAndroid,
    val textures: Array<Bitmap>,
    val physics: L2DPhysics?,
    val pose: L2DPose?,
    val expressions: Array<Live2DExpressionWrapper>?,
    val layoutMatrix: L2DModelMatrix,
    val motionGroups: Array<Live2DMotionGroupWrapper>?,
    val soundPool: SoundPoolWrapper?
) : AutoCloseable {
    override fun close() {
        soundPool?.release()
    }
}
