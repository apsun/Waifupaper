package com.crossbowffs.waifupaper

import android.graphics.Bitmap
import jp.live2d.framework.L2DExpressionMotion
import jp.live2d.framework.L2DPhysics
import jp.live2d.framework.L2DPose
import jp.live2d.motion.Live2DMotion

/**
 * This class holds data for Live2D models that does not depend
 * on an OpenGL context - that is, the data will remain valid
 * even after the context is destroyed. This means that the data
 * does not need to be reloaded between context switches.
 */
class Live2DUnmanagedModelData(
    val textures: Array<Bitmap>,
    val motions: Array<Live2DMotion>,
    val expressions: Array<L2DExpressionMotion>,
    val physics: L2DPhysics,
    val pose: L2DPose
)
