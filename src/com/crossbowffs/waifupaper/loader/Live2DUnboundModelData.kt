package com.crossbowffs.waifupaper.loader

import android.graphics.Bitmap
import jp.live2d.android.Live2DModelAndroid
import jp.live2d.framework.L2DPhysics
import jp.live2d.framework.L2DPose

/**
 * This class holds data for Live2D models that does not depend
 * on an OpenGL context.
 */
class Live2DUnboundModelData(
    val name: String,
    val model: Live2DModelAndroid,
    val textures: Array<Bitmap>,
    val physics: L2DPhysics?,
    val pose: L2DPose?,
    val expressions: Array<Live2DExpressionWrapper>?,
    val motions: Array<Live2DMotionWrapper>?
)
