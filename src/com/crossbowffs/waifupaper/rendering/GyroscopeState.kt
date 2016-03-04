package com.crossbowffs.waifupaper.rendering

class GyroscopeState {
    private val freeRollLimit: Double = 0.5
    private val freePitchLimit: Double = 0.3
    private val maxRevertRate: Double = 20.0
    private val magTimeConst: Double = 0.15
    private val revertTimeConst: Double = 5.0
    private val revertRotConst: Double = 0.15

    private var relativeRoll: Double = 0.0
    private var relativePitch: Double = 0.0

    private var prevTimeStampNs: Long = 0L
    private var magRollAcc: Double = 0.0
    private var magPitchAcc: Double = 0.0
    private var rollRevertRate: Double = 0.0
    private var pitchRevertRate: Double = 0.0
    private var freeRotation: Boolean = true

    fun update(gyroX: Float, gyroY: Float, gyroZ: Float, timeStampNs: Long) {
        if (prevTimeStampNs != 0L) {
            val dTimeSecs = (timeStampNs - prevTimeStampNs) / 1000000000f
            val dRoll = gyroY
            val discriminant = Math.signum(Math.cos(relativeRoll) * gyroX + Math.sin(relativeRoll) * gyroZ)
            val dPitch = discriminant * Math.sqrt((gyroX * gyroX + gyroZ * gyroZ).toDouble())
            val rotMagnitude = Math.sqrt((gyroX * gyroX + gyroY * gyroY + gyroZ * gyroZ).toDouble())

            relativeRoll = fixAngle(relativeRoll + dRoll * dTimeSecs)
            relativePitch = fixAngle(relativePitch + dPitch * dTimeSecs)

            if (magTimeConst < dTimeSecs) {
                magRollAcc = rotMagnitude
                magPitchAcc = rotMagnitude
            } else {
                magRollAcc *= (magTimeConst - dTimeSecs) / magTimeConst
                magRollAcc += Math.signum(relativeRoll) * rotMagnitude * (dTimeSecs / magTimeConst)
                magPitchAcc *= (magTimeConst - dTimeSecs) / magTimeConst
                magPitchAcc += Math.signum(relativePitch) * rotMagnitude * (dTimeSecs / magTimeConst)
            }

            if (Math.abs(relativeRoll) > freeRollLimit || Math.abs(relativePitch) > freePitchLimit) {
                freeRotation = false
            } else if (freeRotation) {
                rollRevertRate = 0.0
                pitchRevertRate = 0.0
                prevTimeStampNs = timeStampNs
                return
            }

            val magTotalAcc: Double = Math.sqrt(0.5 * magRollAcc * magRollAcc + 0.5 * magPitchAcc * magPitchAcc)
            val goalRevertRate: Double = maxRevertRate * Math.exp(-magTotalAcc / revertRotConst)

            if (goalRevertRate > rollRevertRate && dTimeSecs <= revertTimeConst) {
                rollRevertRate *= (revertTimeConst - dTimeSecs) / revertTimeConst
                rollRevertRate += goalRevertRate * (dTimeSecs / revertTimeConst)
            } else {
                rollRevertRate = goalRevertRate
            }

            if (goalRevertRate > pitchRevertRate && dTimeSecs <= revertTimeConst) {
                pitchRevertRate *= (revertTimeConst - dTimeSecs) / revertTimeConst
                pitchRevertRate += goalRevertRate * (dTimeSecs / revertTimeConst)
            } else {
                pitchRevertRate = goalRevertRate
            }

            relativeRoll = moveToward(relativeRoll, 0.0, rollRevertRate * dTimeSecs)
            relativePitch = moveToward(relativePitch, 0.0, pitchRevertRate * dTimeSecs)
            if (relativeRoll == 0.0 && relativePitch == 0.0) {
                freeRotation = true
            }
        }
        prevTimeStampNs = timeStampNs
    }

    fun getRelativeRoll(): Double {
        return relativeRoll
    }

    fun getRelativePitch(): Double {
        return relativePitch
    }

    private fun fixAngle(angle: Double): Double {
        var res = angle
        if (res > Math.PI) {
            res -= 2 * Math.PI
        } else if (res < -Math.PI) {
            res += 2 * Math.PI
        }
        return res
    }

    private fun moveToward(current: Double, goal: Double, stepSize: Double): Double {
        if (Math.abs(current - goal) < Math.abs(stepSize)) {
            return goal
        } else {
            return current - Math.signum(current - goal) * stepSize
        }
    }
}
