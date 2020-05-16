package com.ricko.firecracker.objects

import android.widget.ImageView
import com.google.android.material.imageview.ShapeableImageView

data class Meteor(
    val size: Int,
    val speed: Float,
    var direction: Float,
    var health: Long,
    var currentX: Float,
    var currentY: Float,
    val meteorView: ShapeableImageView
) {
}