package com.crossbowffs.waifupaper.loader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import com.crossbowffs.waifupaper.utils.join
import com.crossbowffs.waifupaper.utils.toByteArray
import jp.live2d.android.Live2DModelAndroid
import jp.live2d.framework.L2DExpressionMotion
import jp.live2d.framework.L2DPhysics
import jp.live2d.framework.L2DPose
import jp.live2d.motion.Live2DMotion
import jp.live2d.util.Json

/**
 * Handles the loading of Live2D model data from disk into memory.
 */
object Live2DModelLoader {
    private fun parseModelJson(fName: String, modelJsonBytes: ByteArray): Live2DModelInfo {
        val modelJson = Json.parseFromBytes(modelJsonBytes)

        val name = modelJson.getAsString("name") ?: fName
        val modelPath = modelJson.getAsString("model")!!
        val texturePaths = modelJson.getAsList("textures")!!.map { it.toString() }.toTypedArray()
        val physicsPath = modelJson.getAsString("physics")
        val posePath = modelJson.getAsString("pose")
        val expressionInfos = modelJson.getAsList("expressions")?.map { expression ->
            val paramMap = expression.toMap()
            Live2DExpressionInfo(paramMap["name"]!!.toString(), paramMap["file"]!!.toString())
        }?.toTypedArray()

        val motionInfos = modelJson.getAsMap("motions")?.map { motion -> Live2DMotionInfo(
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
        )}?.toTypedArray()

        return Live2DModelInfo(
            name,
            modelPath,
            texturePaths,
            physicsPath,
            posePath,
            expressionInfos,
            motionInfos
        )
    }

    private fun loadModel(loader: FileLoader, modelPath: String): Live2DModelAndroid {
        return loader.openStream(modelPath).use { Live2DModelAndroid.loadModel(it) }
    }

    private fun loadTextures(loader: FileLoader, texturePaths: Array<String>): Array<Bitmap> {
        return texturePaths.map {
            loader.openStream(it).use {
                BitmapFactory.decodeStream(it)
            }
        }.toTypedArray()
    }

    private fun loadPhysics(loader: FileLoader, physicsPath: String?): L2DPhysics? {
        if (physicsPath == null) return null
        return loader.openStream(physicsPath).use { L2DPhysics.load(it) }
    }

    private fun loadPose(loader: FileLoader, posePath: String?): L2DPose? {
        if (posePath == null) return null
        return loader.openStream(posePath).use { L2DPose.load(it) }
    }

    private fun loadExpressions(loader: FileLoader, expressionInfos: Array<Live2DExpressionInfo>?): Array<Live2DExpressionWrapper>? {
        if (expressionInfos == null) return null
        return expressionInfos.map { expression ->
            loader.openStream(expression.filePath).use {
                Live2DExpressionWrapper(expression.name, L2DExpressionMotion.loadJson(it))
            }
        }.toTypedArray()
    }

    private fun loadMotions(loader: FileLoader, motionInfos: Array<Live2DMotionInfo>?): Array<Live2DMotionWrapper>? {
        if (motionInfos == null) return null
        return motionInfos.map { motion ->
            Live2DMotionWrapper(motion.name,
                motion.parts.map { params ->
                loader.openStream(params.filePath).use {
                    Live2DSubMotionWrapper(
                        Live2DMotion.loadMotion(it),
                        params.fadeInDuration,
                        params.fadeOutDuration
                    )
                }
            }.toTypedArray())
        }.toTypedArray()
    }

    private fun loadInfo(loader: FileLoader, name: String): Live2DModelInfo {
        try {
            return loader.openStream("$name.model.json").use {
                parseModelJson(name, it.toByteArray())
            }
        } catch (e: Exception) {
            throw Live2DModelLoadException(e)
        }
    }

    private fun load(loader: FileLoader, name: String): Live2DUnboundModelData {
        val modelInfo = try {
            loadInfo(loader, name)
        } catch (e: Exception) {
            throw Live2DModelLoadException(e)
        }
        return load(loader, modelInfo)
    }

    private fun load(loader: FileLoader, modelInfo: Live2DModelInfo): Live2DUnboundModelData {
        try {
            return Live2DUnboundModelData(
                modelInfo.name,
                loadModel(loader, modelInfo.modelPath),
                loadTextures(loader, modelInfo.texturePaths),
                loadPhysics(loader, modelInfo.physicsPath),
                loadPose(loader, modelInfo.posePath),
                loadExpressions(loader, modelInfo.expressionInfos),
                loadMotions(loader, modelInfo.motionInfos)
            )
        } catch (e: Exception) {
            throw Live2DModelLoadException(e)
        }
    }

    /**
     * Loads an unbound Live2D model from external storage.
     *
     * @param name The name of the model.
     */
    fun loadExternal(name: String): Live2DUnboundModelData {
        val extDir = Environment.getExternalStorageDirectory()
        val modelDir = extDir.join("Waifupaper", name)
        val loader = FileLoader.ExternalFileLoader(modelDir)
        return load(loader, name)
    }

    /**
     * Loads an unbound Live2D model from internal storage (assets).
     *
     * @param context A context instance used to access app assets.
     * @param name The name of the model.
     */
    fun loadInternal(context: Context, name: String): Live2DUnboundModelData {
        val loader = FileLoader.AssetFileLoader(context, name)
        return load(loader, name)
    }

    /**
     * Loads information about a Live2D model from external storage.
     * This is useful when enumerating model info without actually
     * loading them into memory.
     *
     * @param name The name of the model.
     */
    fun loadExternalInfo(name: String): Live2DModelInfo {
        val extDir = Environment.getExternalStorageDirectory()
        val modelDir = extDir.join("Waifupaper", name)
        val loader = FileLoader.ExternalFileLoader(modelDir)
        return loadInfo(loader, name)
    }

    /**
     * Loads information about a Live2D model from internal storage (assets).
     * This is useful when enumerating model info without actually
     * loading them into memory.
     *
     * @param context A context instance used to access app assets.
     * @param name The name of the model.
     */
    fun loadInternalInfo(context: Context, name: String): Live2DModelInfo {
        val loader = FileLoader.AssetFileLoader(context, name)
        return loadInfo(loader, name)
    }
}
