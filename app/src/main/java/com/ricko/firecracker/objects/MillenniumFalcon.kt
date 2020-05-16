package com.ricko.firecracker.objects

import android.widget.ImageView
import com.google.android.material.imageview.ShapeableImageView

data class MillenniumFalcon(
    val numberOfBullets: Int,
    val currentX: Float,
    val currentY: Float,
    val millenniumFalconView: ShapeableImageView
) {
}