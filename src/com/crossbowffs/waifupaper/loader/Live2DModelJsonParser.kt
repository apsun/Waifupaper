package com.crossbowffs.waifupaper.loader

import jp.live2d.util.Json

internal fun parseModelJson(modelJsonBytes: ByteArray): Live2DModelInfo {
    val modelJson = Json.parseFromBytes(modelJsonBytes)

    val modelPath = modelJson.getAsString("model")
    val texturePaths = modelJson.getAsList("textures").map { it.toString() }.toTypedArray()
    val physicsPath = modelJson.getAsString("physics")
    val posePath = modelJson.getAsString("pose")
    val expressionInfos = modelJson.getAsList("expressions").map { expression ->
        val paramMap = expression.toMap()
        Live2DExpressionInfo(paramMap["name"]!!.toString(), paramMap["file"]!!.toString())
    }.toTypedArray()

    val motionInfos = modelJson.getAsMap("motions").map { motion -> Live2DMotionInfo(
        motion.key.toString(),
        motion.value.toList().map { motionParams ->
            val paramMap = motionParams.toMap()
            Live2DSubMotionInfo(
                paramMap["file"]!!.toString(),
                paramMap["sound"]?.toString(),
                paramMap["fade_in"]?.toInt(),
                paramMap["fade_out"]?.toInt()
            )
        }.toTypedArray()
    )}.toTypedArray()

    return Live2DModelInfo(
        modelPath,
        texturePaths,
        physicsPath,
        posePath,
        expressionInfos,
        motionInfos
    )
}
