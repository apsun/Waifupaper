package com.crossbowffs.waifupaper.rendering

class SceneTransformManager(
    val screenPivotX: Float,
    val screenPivotY: Float,
    val bgMaxShiftX: Float,
    val bgMaxShiftY: Float) {

    private val modelMaxX: Float = 1f
    private val modelMaxY: Float = 1f
    private val holdDuration: Float = 1.0f
    private val transitionSpeed: Float = 4.0f

    private enum class TransitionStage {
        FOLLOW_TOUCH,
        PAUSE,
        FOLLOW_GYRO
    }

    private val gyroState = GyroscopeState()
    private val maxUserRoll = 1
    private val maxUserPitch = 0.5f
    private val XMaxDiff = Math.min(screenPivotX, 1 - screenPivotX)
    private val YMaxDiff = Math.min(screenPivotY, 1 - screenPivotY)

    private var modelX: Float = 0f
    private var modelY: Float = 0f
    private var bgX: Float = 0f
    private var bgY: Float = 0f

    private var touchFollowState: TransitionStage = TransitionStage.FOLLOW_GYRO
    private var screenTouchX: Float = 0f
    private var screenTouchY: Float = 0f
    private var holdTimer: Float = 0f
    private var prevTimeStampNs: Long = 0L

    fun update(timeStampNs: Long) {
        if (prevTimeStampNs != 0L) {
            val dTimeSecs: Float = (timeStampNs - prevTimeStampNs) / 1000000000f
            if (touchFollowState == TransitionStage.FOLLOW_TOUCH) {
                modelX = moveToward(
                    modelX, modelMaxX * (screenTouchX - screenPivotX) / XMaxDiff,
                    transitionSpeed * modelMaxX * XMaxDiff * dTimeSecs)
                modelY = moveToward(
                    modelY, modelMaxY * (screenPivotY - screenTouchY) / YMaxDiff,
                    transitionSpeed * modelMaxY * YMaxDiff * dTimeSecs)
            } else if (touchFollowState == TransitionStage.PAUSE) {
                holdTimer = moveToward(holdTimer, 0f, dTimeSecs)
                if (holdTimer == 0f) {
                    touchFollowState = TransitionStage.FOLLOW_GYRO
                }
            } else if (touchFollowState == TransitionStage.FOLLOW_GYRO) {
                val targetX = -modelMaxX / maxUserRoll * gyroState.getRelativeRoll().toFloat()
                val targetY = modelMaxY / maxUserPitch * gyroState.getRelativePitch().toFloat()
                modelX = moveToward(modelX, targetX, transitionSpeed * modelMaxX * XMaxDiff * dTimeSecs)
                modelY = moveToward(modelY, targetY, transitionSpeed * modelMaxY * YMaxDiff * dTimeSecs)
            }
            bgX = clamp(
                -bgMaxShiftX * gyroState.getRelativeRoll().toFloat(),
                -bgMaxShiftX, bgMaxShiftX)
            bgY = clamp(
                bgMaxShiftY * gyroState.getRelativePitch().toFloat(),
                -bgMaxShiftY, bgMaxShiftY)
        }
        prevTimeStampNs = timeStampNs
    }

    fun getModelXY(): Point2D {
        return Point2D(modelX, modelY)
    }

    fun getBGShiftXY(): Point2D {
        return Point2D(bgX, bgY)
    }

    fun touchDown(normalizedX: Float, normalizedY: Float) {
        touchFollowState = TransitionStage.FOLLOW_TOUCH
        screenTouchX = normalizedX
        screenTouchY = normalizedY
    }

    fun touchChanged(normalizedX: Float, normalizedY: Float) {
        screenTouchX = normalizedX
        screenTouchY = normalizedY
    }

    fun touchUp() {
        touchFollowState = TransitionStage.PAUSE
        holdTimer = holdDuration
    }

    fun gyroChanged(gyroX: Float, gyroY: Float, gyroZ: Float, timeStampNs: Long) {
        gyroState.update(gyroX, gyroY, gyroZ, timeStampNs)
    }

    private fun moveToward(current: Float, goal: Float, stepSize: Float): Float {
        if (Math.abs(current - goal) < Math.abs(stepSize)) {
            return goal
        } else {
            return current - Math.signum(current - goal) * stepSize
        }
    }

    private fun clamp(value: Float, min: Float, max: Float): Float {
        return Math.max(Math.min(value, max), min)
    }
}
