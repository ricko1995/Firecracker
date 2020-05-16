package com.ricko.firecracker

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.DisplayMetrics
import android.view.View
import android.view.WindowManager
import androidx.core.content.ContextCompat
import com.google.android.material.imageview.ShapeableImageView
import com.ricko.firecracker.objects.Meteor
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private val meteors: ArrayList<Meteor> = ArrayList()

    private val displayMetrics = DisplayMetrics()
    private var startStop = false
    private lateinit var job: Job
    private var initialMeteorX = 0f
    private var initialMeteorY = 0f

    companion object {
        const val NUMBER_OF_METEORS = 2000
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        windowManager.defaultDisplay.getMetrics(displayMetrics)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        initialMeteorX = displayMetrics.widthPixels / 2f
        createMeteors()

        updateBtn.setOnClickListener {
            startStop = !startStop
            if (startStop) {
                job = CoroutineScope(Main).launch {
                    while (true) {
                        updateFrame()
                        delay(30)
                    }
                }
            } else job.cancel()
        }

        resetBtn.setOnClickListener {
            resetMeteors()
        }

    }

    private fun createMeteors() {
        for (i in 0 until NUMBER_OF_METEORS) {
            val meteorView = ShapeableImageView(this)
            meteorView.id = View.generateViewId()
            meteorView.background =
                ContextCompat.getDrawable(applicationContext, R.drawable.ic_launcher_background)
            meteors.add(
                Meteor(
                    Random.nextInt(1, 2) * 20,
                    Random.nextInt(1, 10).toFloat(),
                    Random.nextInt(30, 150) * PI.toFloat() / 180,
                    Random.nextLong(1, 100),
                    initialMeteorX,
                    -500f,
                    meteorView
                )
            )
            gameLayout.addView(meteors[i].meteorView)
            meteors[i].meteorView.layoutParams.height = meteors[i].size
            meteors[i].meteorView.layoutParams.width = meteors[i].size
            meteors[i].meteorView.x = meteors[i].currentX
            meteors[i].meteorView.y = meteors[i].currentY

            meteors[i].meteorView.setOnClickListener {
                gameLayout.removeView(meteors[i].meteorView)
//                meteors.remove(meteors[i])
            }
        }
    }

    private fun updateFrame() {
        for (meteor in meteors) {
            if (meteor.meteorView.x + meteor.meteorView.layoutParams.width > displayMetrics.widthPixels.toFloat()) {
                meteor.currentX =
                    displayMetrics.widthPixels.toFloat() - meteor.meteorView.layoutParams.width
                meteor.direction = PI.toFloat() - meteor.direction
                if (meteor.direction > 2 * PI.toFloat()) {
                    meteor.direction -= 2 * PI.toFloat()
                }
                moveMeteor(meteor)
            } else if (meteor.meteorView.x < 0f) {
                meteor.currentX = 0f
                meteor.direction = PI.toFloat() - meteor.direction
                if (meteor.direction < 0) {
                    meteor.direction += 2 * PI.toFloat()
                }
                moveMeteor(meteor)
            }
            else if (meteor.meteorView.y-meteor.meteorView.layoutParams.height> displayMetrics.heightPixels.toFloat()){
                meteor.currentY=-300f
                moveMeteor(meteor)
            }
            else {
                moveMeteor(meteor)
            }

        }
    }

    private fun moveMeteor(meteor: Meteor) {
        meteor.currentX += meteor.speed * cos(meteor.direction)
        meteor.currentY += meteor.speed * sin(meteor.direction)
        meteor.meteorView.x = meteor.currentX
        meteor.meteorView.y = meteor.currentY
    }

    private fun resetMeteors() {
        for (meteor in meteors) {
            meteor.currentX = initialMeteorX
            meteor.currentY = initialMeteorY
        }
    }
}

