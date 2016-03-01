package com.crossbowffs.waifupaper.loader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.crossbowffs.waifupaper.utils.*
import jp.live2d.android.Live2DModelAndroid
import jp.live2d.framework.L2DExpressionMotion
import jp.live2d.framework.L2DModelMatrix
import jp.live2d.framework.L2DPhysics
import jp.live2d.framework.L2DPose
import jp.live2d.motion.Live2DMotion
import jp.live2d.util.Json
import java.io.File

/**
 * Where external assets are stored, relative to the external storage
 * base directory.
 */
private const val EXTERNAL_DIR_NAME: String = "Waifupaper"

/**
 * Where models are stored, relative to the loader base directory.
 */
private const val MODELS_DIR_NAME: String = "Models"

/**
 * Handles the loading of Live2D model data from disk into memory.
 */
object Live2DModelLoader {
    private fun parseModelJson(location: FileLocation, fName: String, modelJsonBytes: ByteArray): Live2DModelInfo {
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
        val layoutInfo = modelJson.getAsMap("layout")?.map { it.key to it.value.toFloat() }?.toMap()
        val motionGroupInfos = modelJson.getAsMap("motions")?.map { motionGroup ->
            Live2DMotionGroupInfo(
                motionGroup.key.toString(),
                motionGroup.value.toList().map { motionParams ->
                    val paramMap = motionParams.toMap()
                    Live2DMotionInfo(
                        paramMap["file"]!!.toString(),
                        paramMap["sound"]?.toString(),
                        paramMap["fade_in"]?.toInt(),
                        paramMap["fade_out"]?.toInt()
                    )
                }.toTypedArray()
            )
        }?.toTypedArray()

        return Live2DModelInfo(
            location,
            name,
            modelPath,
            texturePaths,
            physicsPath,
            posePath,
            expressionInfos,
            layoutInfo,
            motionGroupInfos
        )
    }

    private fun loadModel(loader: FileLoaderWrapper, modelPath: String): Live2DModelAndroid {
        return loader.openStream(modelPath).use { Live2DModelAndroid.loadModel(it) }
    }

    private fun loadTextures(loader: FileLoaderWrapper, texturePaths: Array<String>): Array<Bitmap> {
        return texturePaths.map {
            loader.openStream(it).use {
                BitmapFactory.decodeStream(it)
            }
        }.toTypedArray()
    }

    private fun loadPhysics(loader: FileLoaderWrapper, physicsPath: String?): L2DPhysics? {
        if (physicsPath == null) return null
        return loader.openStream(physicsPath).use { L2DPhysics.load(it) }
    }

    private fun loadPose(loader: FileLoaderWrapper, posePath: String?): L2DPose? {
        if (posePath == null) return null
        return loader.openStream(posePath).use { L2DPose.load(it) }
    }

    private fun loadExpressions(loader: FileLoaderWrapper, expressionInfos: Array<Live2DExpressionInfo>?): Array<Live2DExpressionWrapper>? {
        if (expressionInfos == null) return null
        return expressionInfos.map { expression ->
            loader.openStream(expression.filePath).use {
                Live2DExpressionWrapper(expression.name, L2DExpressionMotion.loadJson(it))
            }
        }.toTypedArray()
    }

    private fun parseLayout(model: Live2DModelAndroid, layoutInfo: Map<String, Float>?): L2DModelMatrix {
        val matrix = L2DModelMatrix(model.canvasWidth, model.canvasHeight)
        matrix.setWidth(2f)
        matrix.setCenterPosition(0f, 0f)
        if (layoutInfo != null) {
            layoutInfo["width"].useNotNull { matrix.setWidth(it) }
            layoutInfo["height"].useNotNull { matrix.setHeight(it) }
            layoutInfo["x"].useNotNull { matrix.setX(it) }
            layoutInfo["y"].useNotNull { matrix.setY(it) }
            layoutInfo["center_x"].useNotNull { matrix.centerX(it) }
            layoutInfo["center_y"].useNotNull { matrix.centerY(it) }
            layoutInfo["top"].useNotNull { matrix.top(it) }
            layoutInfo["bottom"].useNotNull { matrix.bottom(it) }
            layoutInfo["left"].useNotNull { matrix.left(it) }
            layoutInfo["right"].useNotNull { matrix.right(it) }
        }
        return matrix
    }

    private fun loadMotionsAndSound(loader: FileLoaderWrapper,
                                    motionGroupInfos: Array<Live2DMotionGroupInfo>?,
                                    loadSound: Boolean): Pair<Array<Live2DMotionGroupWrapper>, SoundPoolWrapper?>? {
        // TODO: Clean up this method
        if (motionGroupInfos == null) return null
        val soundPool = if (loadSound) SoundPoolWrapper() else null
        return Pair(motionGroupInfos.map { motion ->
            Live2DMotionGroupWrapper(
                motion.name,
                motion.motions.map { params ->
                    loader.openStream(params.filePath).use {
                        Live2DMotionWrapper(
                            Live2DMotion.loadMotion(it),
                            soundPool.useNotNull { sp ->
                                params.soundFilePath.useNotNull {
                                    sp.loadSound(loader, it)
                                }
                            },
                            params.fadeInDuration,
                            params.fadeOutDuration
                        )
                    }
                }.toTypedArray())
        }.toTypedArray(), soundPool)
    }

    private fun loadInfo(loader: FileLoaderWrapper, name: String): Live2DModelInfo {
        try {
            return loader.openStream("$name.model.json").use {
                parseModelJson(loader.location, name, it.toByteArray())
            }
        } catch (e: Exception) {
            throw Live2DModelLoadException(e)
        }
    }

    private fun load(loader: FileLoaderWrapper, modelInfo: Live2DModelInfo, loadSound: Boolean): Live2DModelWrapper {
        try {
            val model = loadModel(loader, modelInfo.modelPath)
            val motionsAndSound = loadMotionsAndSound(loader, modelInfo.motionGroupInfos, loadSound)
            return Live2DModelWrapper(
                modelInfo.name,
                model,
                loadTextures(loader, modelInfo.texturePaths),
                loadPhysics(loader, modelInfo.physicsPath),
                loadPose(loader, modelInfo.posePath),
                loadExpressions(loader, modelInfo.expressionInfos),
                parseLayout(model, modelInfo.layoutInfo),
                motionsAndSound.useNotNull { it.first },
                motionsAndSound.useNotNull { it.second }
            )
        } catch (e: Exception) {
            throw Live2DModelLoadException(e)
        }
    }

    private fun load(loader: FileLoaderWrapper, name: String, loadSound: Boolean): Live2DModelWrapper {
        val modelInfo = try {
            loadInfo(loader, name)
        } catch (e: Exception) {
            throw Live2DModelLoadException(e)
        }
        return load(loader, modelInfo, loadSound)
    }

    private fun wrapModelLoader(loader: FileLoader, name: String): FileLoaderWrapper {
        return FileLoaderWrapper(loader, File(MODELS_DIR_NAME, name).path)
    }

    /**
     * Loads an unbound Live2D model from external storage.
     *
     * @param name The name of the model.
     * @param loadSound Whether to load sound resources.
     */
    fun loadExternal(name: String, loadSound: Boolean): Live2DModelWrapper {
        val loader = wrapModelLoader(ExternalFileLoader(EXTERNAL_DIR_NAME), name)
        return load(loader, name, loadSound)
    }

    /**
     * Loads an unbound Live2D model from internal storage (assets).
     *
     * @param context A context instance used to access app assets.
     * @param name The name of the model.
     * @param loadSound Whether to load sound resources.
     */
    fun loadInternal(context: Context, name: String, loadSound: Boolean): Live2DModelWrapper {
        val loader = wrapModelLoader(AssetFileLoader(context), name)
        return load(loader, name, loadSound)
    }

    /**
     * Loads information about a Live2D model from external storage.
     * This is useful when enumerating model info without actually
     * loading them into memory.
     *
     * @param name The name of the model.
     */
    fun loadExternalInfo(name: String): Live2DModelInfo {
        val loader = wrapModelLoader(ExternalFileLoader(EXTERNAL_DIR_NAME), name)
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
        val loader = wrapModelLoader(AssetFileLoader(context), name)
        return loadInfo(loader, name)
    }

    /**
     * Enumerates all available model data from both internal and external
     * storage. Models that fail to load will be silently ignored.
     *
     * @param context A context instance used to access app assets.
     */
    fun enumerateModels(context: Context, loadExternal: Boolean): Array<Live2DModelInfo> {
        val internalLoader = AssetFileLoader(context)
        val externalLoader = ExternalFileLoader(EXTERNAL_DIR_NAME)
        var models = internalLoader.enumerate(MODELS_DIR_NAME).map {
            loadInternalInfo(context, it)
        }
        if (loadExternal) {
            models = models.plus(externalLoader.enumerate(MODELS_DIR_NAME).mapNotNull {
                try {
                    loadExternalInfo(it)
                } catch (e: Exception) {
                    loge("Failed to load model", e)
                    null
                }
            })
        }
        return models.toTypedArray()
    }
}
