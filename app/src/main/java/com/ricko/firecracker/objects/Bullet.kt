package com.ricko.firecracker.objects

import android.widget.ImageView

data class Bullet(
    val speed: Float,
    val damage: Long,
    val currentX: Float,
    var currentY: Float,
    val bulletView: ImageView
) {
    fun moveBullet() {
        currentY -= speed
        bulletView.y = currentY
    }

    fun setBullet() {
        bulletView.layoutParams.height = 50
        bulletView.layoutParams.width = 25
        bulletView.x = currentX - bulletView.layoutParams.width/2
        bulletView.y = currentY
    }
}