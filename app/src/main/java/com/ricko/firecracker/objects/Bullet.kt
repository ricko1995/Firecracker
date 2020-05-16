package com.ricko.firecracker.objects

import android.widget.ImageView
import com.google.android.material.imageview.ShapeableImageView

data class Bullet(
    val speed: Float,
    val damage: Long,
    val currentX: Float,
    var currentY: Float,
    val bulletView: ShapeableImageView
){
    fun moveBullet(){
        currentY -= speed
        bulletView.y = currentY
    }
    fun setBullet(){
        bulletView.layoutParams.height = 50
        bulletView.layoutParams.width = 25
        bulletView.x = currentX
        bulletView.y = currentY
    }
}