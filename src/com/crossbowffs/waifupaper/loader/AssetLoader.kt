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
 * Where background images are stored, relative to the loader base directory.
 */
private const val BACKGROUNDS_DIR_NAME: String = "Backgrounds"

/**
 * Handles the loading of Live2D model data from disk into memory.
 */
object AssetLoader {
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

    private fun loadMotions(loader: FileLoaderWrapper, motionGroupInfos: Array<Live2DMotionGroupInfo>?): Array<Live2DMotionGroupWrapper>? {
        if (motionGroupInfos == null) return null
        return motionGroupInfos.map { motion ->
            Live2DMotionGroupWrapper(
                motion.name,
                motion.motions.map { params ->
                    loader.openStream(params.filePath).use {
                        Live2DMotionWrapper(
                            Live2DMotion.loadMotion(it),
                            params.soundFilePath,
                            params.fadeInDuration,
                            params.fadeOutDuration
                        )
                    }
                }.toTypedArray()
            )
        }.toTypedArray()
    }

    private fun loaderFor(context: Context, location: FileLocation) = when (location) {
        FileLocation.INTERNAL -> InternalFileLoader(context)
        FileLocation.EXTERNAL -> ExternalFileLoader(EXTERNAL_DIR_NAME)
    }

    private fun wrapModelLoader(loader: FileLoader, name: String): FileLoaderWrapper {
        return FileLoaderWrapper(loader, File(MODELS_DIR_NAME, name).path)
    }

    /**
     * Loads information about a Live2D model.
     * This is useful when enumerating model info without actually
     * loading them into memory.
     *
     * @param context A context instance used to access app assets.
     * @param name The name of the model.
     * @param location The location to look for the model in.
     */
    fun loadModelInfo(context: Context, name: String, location: FileLocation): Live2DModelInfo {
        val loader = wrapModelLoader(loaderFor(context, location), name)
        return loader.openStream("$name.model.json").use {
            parseModelJson(loader.location, name, it.toByteArray())
        }
    }

    /**
     * Loads an unbound Live2D model.
     *
     * @param context A context instance used to access app assets.
     * @param modelInfo The model to load.
     */
    fun loadModel(context: Context, modelInfo: Live2DModelInfo): Live2DModelWrapper {
        val loader = wrapModelLoader(loaderFor(context, modelInfo.location), modelInfo.name)
        val model = loadModel(loader, modelInfo.modelPath)
        return Live2DModelWrapper(
            modelInfo.name,
            model,
            loadTextures(loader, modelInfo.texturePaths),
            loadPhysics(loader, modelInfo.physicsPath),
            loadPose(loader, modelInfo.posePath),
            loadExpressions(loader, modelInfo.expressionInfos),
            parseLayout(model, modelInfo.layoutInfo),
            loadMotions(loader, modelInfo.motionGroupInfos)
        )
    }

    /**
     * Loads Live2D motion sounds into a sound pool. You must release
     * the pool once you are done using it. If the model
     * does have any motion groups, {@code null} will be returned.
     *
     * @param modelInfo The model to load sounds for.
     */
    fun loadSounds(context: Context, modelInfo: Live2DModelInfo): SoundPoolWrapper? {
        val loader = wrapModelLoader(loaderFor(context, modelInfo.location), modelInfo.name)
        if (modelInfo.motionGroupInfos == null) return null
        val soundPool = SoundPoolWrapper()
        modelInfo.motionGroupInfos.forEach {
            it.motions.forEach {
                it.soundFilePath.useNotNull { soundPool.loadSound(loader, it) }
            }
        }
        return soundPool
    }

    /**
     * Loads a background image.
     *
     * @param context A context instance used to access app assets.
     * @param imagePath The path to background image to load, relative to
     *                  the "Backgrounds" directory.
     */
    fun loadBackground(context: Context, imagePath: String, location: FileLocation): Bitmap {
        val loader = FileLoaderWrapper(loaderFor(context, location), BACKGROUNDS_DIR_NAME)
        return loader.openStream(imagePath).use {
            BitmapFactory.decodeStream(it)
        }
    }

    /**
     * Enumerates all available model data from both internal and external
     * storage. Models that fail to load will be silently ignored.
     *
     * @param context A context instance used to access app assets.
     */
    fun enumerateModels(context: Context, loadExternal: Boolean): Array<Live2DModelInfo> {
        val internalLoader = InternalFileLoader(context)
        val externalLoader = ExternalFileLoader(EXTERNAL_DIR_NAME)
        var models = internalLoader.enumerate(MODELS_DIR_NAME).map {
            loadModelInfo(context, it, FileLocation.INTERNAL)
        }
        if (loadExternal) {
            models = models.plus(externalLoader.enumerate(MODELS_DIR_NAME).mapNotNull {
                try {
                    loadModelInfo(context, it, FileLocation.EXTERNAL)
                } catch (e: Exception) {
                    loge("Failed to load model", e)
                    null
                }
            })
        }
        return models.toTypedArray()
    }
}
