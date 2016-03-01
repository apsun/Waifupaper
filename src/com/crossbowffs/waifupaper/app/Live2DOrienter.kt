package com.crossbowffs.waifupaper.app

class Live2DOrienter(
        val screenPivotX: Float,
        val screenPivotY: Float,
        val modelMaxX: Float,
        val modelMaxY: Float,
        val holdDuration: Float,
        val transitionSpeed: Float) {

    private enum class TransitionStage {
        FOLLOW_TOUCH,
        PAUSE,
        REVERT_TO_NEUTRAL,
        FOLLOW_GYRO;
    }

    private val gyroReader = RelativeRotationGyro(0.3, 0.2, 5.0, 0.15, 4.0, 0.15)
    private val maxUserRoll = 1
    private val maxUserPitch = 0.5f
    private val XMaxDiff = Math.max(screenPivotX, 1 - screenPivotX)
    private val YMaxDiff = Math.max(screenPivotY, 1 - screenPivotY)

    private var modelX: Float = 0f
    private var modelY: Float = 0f

    private var touchFollowState: TransitionStage = TransitionStage.FOLLOW_GYRO
    private var screenTouchX: Float = 0f
    private var screenTouchY: Float = 0f
    private var holdTimer: Float = 0f
    private var prevTimeStampNs: Long = 0L

    fun getModelOrientation(timeStampNs: Long): Pair<Float, Float> {
        if (prevTimeStampNs != 0L) {
            val dTimeSecs: Float = (timeStampNs - prevTimeStampNs) / 1000000000f
            if (touchFollowState == TransitionStage.FOLLOW_TOUCH) {
                modelX = moveToward(modelX,
                        modelMaxX * (screenTouchX - screenPivotX) / XMaxDiff , transitionSpeed)
                modelY = moveToward(modelY,
                        modelMaxY * (screenTouchY - screenPivotY) / YMaxDiff , transitionSpeed)
            } else if (touchFollowState == TransitionStage.PAUSE) {
                holdTimer = moveToward(holdTimer, 0f, dTimeSecs)
                if (holdTimer == 0f) {
                    touchFollowState = TransitionStage.REVERT_TO_NEUTRAL
                }
            } else if (touchFollowState == TransitionStage.REVERT_TO_NEUTRAL) {
                modelX = moveToward(modelX, 0f, transitionSpeed * dTimeSecs)
                modelY = moveToward(modelY, 0f, transitionSpeed * dTimeSecs)
                if (modelX == 0f && modelY == 0f) {
                    touchFollowState = TransitionStage.FOLLOW_GYRO
                }
            } else if (touchFollowState == TransitionStage.FOLLOW_GYRO) {
                modelX = -modelMaxX / maxUserRoll * gyroReader.getRelativeRoll().toFloat()
                modelY = modelMaxY / maxUserPitch * gyroReader.getRelativePitch().toFloat()
            }
        }
        prevTimeStampNs = timeStampNs
        return Pair(modelX, modelY)
    }

    fun touchDown(normalizedX: Float, normalizedY: Float) {
        touchFollowState = TransitionStage.FOLLOW_TOUCH
        gyroReader.reset()
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
        if (touchFollowState == TransitionStage.FOLLOW_GYRO) {
            gyroReader.update(gyroX, gyroY, gyroZ, timeStampNs)
        }
    }

    private fun moveToward(current: Float, goal: Float, stepSize: Float): Float {
        if (Math.abs(current - goal) < Math.abs(stepSize)) {
            return goal
        } else {
            return current - Math.signum(current - goal) * stepSize
        }
    }
}