package com.crossbowffs.waifupaper.loader

data class Live2DModelInfo(
    val modelPath: String,
    val texturePaths: Array<String>,
    val physicsPath: String,
    val posePath: String,
    val expressionInfos: Array<Live2DExpressionInfo>,
    val motionInfos: Array<Live2DMotionInfo>
)
