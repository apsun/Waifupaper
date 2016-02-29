package com.crossbowffs.waifupaper.loader

data class Live2DMotionInfo(
    val filePath: String,
    val soundFilePath: String?,
    val fadeInDuration: Int?,
    val fadeOutDuration: Int?
)

data class Live2DMotionGroupInfo(
    val name: String,
    val motions: Array<Live2DMotionInfo>
)

data class Live2DExpressionInfo(
    val name: String,
    val filePath: String
)

data class Live2DModelInfo(
    val location: FileLocation,
    val name: String,
    val modelPath: String,
    val texturePaths: Array<String>,
    val physicsPath: String?,
    val posePath: String?,
    val expressionInfos: Array<Live2DExpressionInfo>?,
    val layoutInfo: Map<String, Float>?,
    val motionGroupInfos: Array<Live2DMotionGroupInfo>?
)
