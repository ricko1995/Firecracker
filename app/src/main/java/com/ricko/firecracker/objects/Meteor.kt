package com.ricko.firecracker.objects

import android.widget.ImageView
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class Meteor(
    val size: Int,
    val speed: Float,
    var direction: Float,
    var health: Long,
    var currentX: Float,
    var currentY: Float,
    val meteorView: ImageView
) {
    fun moveMeteor() {
        currentX += speed * cos(direction)
        currentY += speed * sin(direction)
        meteorView.x = currentX
        meteorView.y = currentY
    }

    fun checkBoundaryHit(screenWidth: Int, screenHeight: Int, meteor: Meteor): Meteor? {
        var meteorToRemove: Meteor? = null
        when {
            meteor.meteorView.x + meteor.meteorView.layoutParams.width > screenWidth.toFloat() -> {
                meteor.currentX =
                    screenWidth.toFloat() - meteor.meteorView.layoutParams.width
                meteor.direction = PI.toFloat() - meteor.direction
                if (meteor.direction < 0f) {
                    meteor.direction += 2 * PI.toFloat()
                }
                meteor.moveMeteor()
            }
            meteor.meteorView.x < 0f -> {
                meteor.currentX = 0f
                meteor.direction = PI.toFloat() - meteor.direction
                if (meteor.direction < 0) {
                    meteor.direction += 2 * PI.toFloat()
                }
                meteor.moveMeteor()
            }
            meteor.meteorView.y < 0f && meteor.direction > PI.toFloat() -> {
                meteor.direction = 2 * PI.toFloat() - meteor.direction
                if (meteor.direction < 0) {
                    meteor.direction += 2 * PI.toFloat()
                }
                meteor.moveMeteor()
            }
            meteor.meteorView.y - 150f > screenHeight.toFloat() -> {
                meteorToRemove = meteor
                println("met" + meteor.meteorView.y)
            }
            else -> {
                meteor.moveMeteor()
            }
        }
        return meteorToRemove
    }
}