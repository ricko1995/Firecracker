package com.ricko.firecracker.objects

import android.widget.ImageView
import com.google.android.material.imageview.ShapeableImageView

data class Bullet(
    val speed: Int,
    val damage: Long,
    val currentX: Float,
    val currentY: Float,
    val bulletView: ShapeableImageView
){
}