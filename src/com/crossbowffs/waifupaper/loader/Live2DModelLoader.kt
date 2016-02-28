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

object Live2DModelLoader {
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

    private fun loadPhysics(loader: FileLoader, physicsPath: String): L2DPhysics {
        return loader.openStream(physicsPath).use { L2DPhysics.load(it) }
    }

    private fun loadPose(loader: FileLoader, posePath: String): L2DPose {
        return loader.openStream(posePath).use { L2DPose.load(it) }
    }

    private fun loadExpressions(loader: FileLoader, expressionInfos: Array<Live2DExpressionInfo>): Array<Live2DExpressionWrapper> {
        return expressionInfos.map { expression ->
            loader.openStream(expression.filePath).use {
                Live2DExpressionWrapper(expression.name, L2DExpressionMotion.loadJson(it))
            }
        }.toTypedArray()
    }

    private fun loadMotions(loader: FileLoader, motionInfos: Array<Live2DMotionInfo>): Array<Live2DMotionWrapper> {
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

    private fun load(loader: FileLoader, name: String): Live2DUnboundModelData {
        try {
            val modelInfo = loadInfo(loader, name)
            return Live2DUnboundModelData(
                name,
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

    private fun loadInfo(loader: FileLoader, name: String): Live2DModelInfo {
        try {
            return loader.openStream("$name.model.json").use {
                parseModelJson(it.toByteArray())
            }
        } catch (e: Exception) {
            throw Live2DModelLoadException(e)
        }
    }

    fun loadExternal(name: String): Live2DUnboundModelData {
        val extDir = Environment.getExternalStorageDirectory()
        val modelDir = extDir.join("Waifupaper", name)
        val loader = FileLoader.ExternalFileLoader(modelDir)
        return load(loader, name)
    }

    fun loadInternal(context: Context, name: String): Live2DUnboundModelData {
        val loader = FileLoader.AssetFileLoader(context)
        return load(loader, name)
    }

    fun loadExternalInfo(name: String): Live2DModelInfo {
        val extDir = Environment.getExternalStorageDirectory()
        val modelDir = extDir.join("Waifupaper", name)
        val loader = FileLoader.ExternalFileLoader(modelDir)
        return loadInfo(loader, name)
    }

    fun loadInternalInfo(context: Context, name: String): Live2DModelInfo {
        val loader = FileLoader.AssetFileLoader(context)
        return loadInfo(loader, name)
    }
}
