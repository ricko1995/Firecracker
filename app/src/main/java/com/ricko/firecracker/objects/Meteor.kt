package com.ricko.firecracker.objects

import android.widget.ImageView
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
    fun moveMeteor(){
        currentX += speed * cos(direction)
        currentY += speed * sin(direction)
        meteorView.x = currentX
        meteorView.y = currentY
    }
}