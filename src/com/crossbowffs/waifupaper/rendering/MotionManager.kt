package com.crossbowffs.waifupaper.rendering

import com.crossbowffs.waifupaper.loader.Live2DModelWrapper
import com.crossbowffs.waifupaper.loader.Live2DMotionWrapper
import com.crossbowffs.waifupaper.utils.useNotNull
import jp.live2d.motion.Live2DMotion
import jp.live2d.motion.MotionQueueManager

class MotionManager private constructor(val modelWrapper: Live2DModelWrapper, private val soundCallback: (String) -> Unit) {
    companion object {
        fun of(modelWrapper: Live2DModelWrapper?, soundCallback: (String) -> Unit): MotionManager? {
            if (modelWrapper == null) return null
            if (modelWrapper.motionGroups == null) return null
            if (modelWrapper.motionGroups.sumBy { it.motions.size } == 0) return null
            return MotionManager(modelWrapper, soundCallback)
        }
    }

    private val motionQueue: MotionQueueManager = MotionQueueManager()
    private val model = modelWrapper.model
    private val motionGroups = modelWrapper.motionGroups!!
    private var motionGroupIndex: Int = 0
    private var motionIndex: Int = 0

    fun update() {
        model.loadParam()
        if (motionQueue.isFinished) {
            motionQueue.startMotion(chooseNextMotion(), false)
        } else {
            motionQueue.updateParam(model)
        }
        model.saveParam()
    }

    fun chooseNextMotion(): Live2DMotion {
        var currMotion: Live2DMotionWrapper?
        do {
            var currMotionGroup = motionGroups[motionGroupIndex]
            if (++motionIndex == currMotionGroup.motions.size) {
                motionIndex = 0
                motionGroupIndex = (motionGroupIndex + 1) % motionGroups.size
            }
            currMotion = motionGroups[motionGroupIndex].motions.getOrNull(motionIndex)
        } while (currMotion == null)
        currMotion.soundFilePath.useNotNull { soundCallback(it) }
        return currMotion.motion
    }

    fun stopMotion() {
        motionQueue.stopAllMotions()
    }
}
