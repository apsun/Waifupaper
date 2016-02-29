package com.crossbowffs.waifupaper.app

class RelativeRotationGyro {
    private val freeRollLimit: Double
    private val freePitchLimit: Double
    private val maxRevertRate: Double
    private val magTimeConst: Double
    private val revertTimeConst: Double
    private val revertRotConst: Double

    private var relativeRoll: Double = 0.0
    private var relativePitch: Double = 0.0

    private var prevTimeStamp: Long = 0L
    private var magRollAcc: Double = 0.0
    private var magPitchAcc: Double = 0.0
    private var rollRevertRate: Double = 0.0
    private var pitchRevertRate: Double = 0.0
    private var freeRotation: Boolean = true

    constructor(freeRollLimit: Double, freePitchLimit: Double, maxRevertRate: Double,
                magTimeConst: Double, revertTimeConst: Double, revertRotConst: Double) {
        this.freeRollLimit = freeRollLimit
        this.freePitchLimit = freePitchLimit
        this.maxRevertRate = maxRevertRate
        this.magTimeConst = magTimeConst
        this.revertTimeConst = revertTimeConst
        this.revertRotConst = revertRotConst
    }

    fun update(gyroX: Float, gyroY: Float, gyroZ: Float, timeStamp: Long) {
        if (prevTimeStamp != 0L) {
            val dTime = 0.000000001f * (timeStamp - prevTimeStamp)
            val dRoll = gyroY
            val discriminant = Math.signum(Math.cos(relativeRoll) * gyroX +
                    Math.sin(relativeRoll) * gyroZ)
            val dPitch =
                    discriminant * Math.sqrt((gyroX * gyroX + gyroZ * gyroZ).toDouble())
            val rotMagnitude =
                    Math.sqrt((gyroX * gyroX + gyroY * gyroY + gyroZ * gyroZ).toDouble())

            relativeRoll = fixAngle(relativeRoll + dRoll * dTime)
            relativePitch = fixAngle(relativePitch + dPitch * dTime)

            if (magTimeConst < dTime) {
                magRollAcc = rotMagnitude
                magPitchAcc = rotMagnitude
            } else {
                magRollAcc *= (magTimeConst - dTime) / magTimeConst
                magRollAcc += Math.signum(relativeRoll) *
                        rotMagnitude * (dTime / magTimeConst)
                magPitchAcc *= (magTimeConst - dTime) / magTimeConst
                magPitchAcc += Math.signum(relativePitch) *
                        rotMagnitude * (dTime / magTimeConst)
            }

            if (Math.abs(relativeRoll) > freeRollLimit ||
                    Math.abs(relativePitch) > freePitchLimit) {
                freeRotation = false
            } else if (freeRotation) {
                rollRevertRate = 0.0
                pitchRevertRate = 0.0
                prevTimeStamp = timeStamp
                return
            }

            val magTotalAcc: Double = Math.sqrt(0.5 * magRollAcc * magRollAcc +
                    0.5 * magPitchAcc * magPitchAcc)
            val goalRevertRate: Double =
                    maxRevertRate * Math.exp(-magTotalAcc / revertRotConst)

            if (goalRevertRate > rollRevertRate && dTime <= revertTimeConst) {
                rollRevertRate *= (revertTimeConst - dTime) / revertTimeConst
                rollRevertRate += goalRevertRate * (dTime / revertTimeConst)
            } else {
                rollRevertRate = goalRevertRate
            }
            if (goalRevertRate > pitchRevertRate && dTime <= revertTimeConst) {
                pitchRevertRate *= (revertTimeConst - dTime) / revertTimeConst
                pitchRevertRate += goalRevertRate * (dTime / revertTimeConst)
            } else {
                pitchRevertRate = goalRevertRate
            }

            relativeRoll = moveToward(relativeRoll, 0.0, rollRevertRate * dTime)
            relativePitch = moveToward(relativePitch, 0.0, pitchRevertRate * dTime)
            if (relativeRoll == 0.0 && relativePitch == 0.0) {
                freeRotation = true
            }
        }
        prevTimeStamp = timeStamp
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