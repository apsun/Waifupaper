package com.crossbowffs.waifupaper

data class Live2DModelInfo(
    val modelPath: String,
    val texturePaths: Array<String>,
    val motionPaths: Array<String>,
    val physicsPath: String
)
