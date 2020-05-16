package com.ricko.firecracker

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.google.android.material.imageview.ShapeableImageView
import com.ricko.firecracker.objects.Bullet
import com.ricko.firecracker.objects.Meteor
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
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
    private val bullets: ArrayList<Bullet> = ArrayList()

    private val displayMetrics = DisplayMetrics()
    private var isStarted = false
    private var gameJob: Job? = null
    private var initialMeteorX = 0f
    private var initialMeteorY = 0f
    private var clickCount = 0

    companion object {
        const val NUMBER_OF_METEORS = 50
        const val GAME_FPS = 60L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        windowManager.defaultDisplay.getMetrics(displayMetrics)

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
//            window.attributes.layoutInDisplayCutoutMode =
//                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
//        }
        initialMeteorX = displayMetrics.widthPixels / 2f
        createMeteors()

        updateBtn.setOnClickListener {
            isStarted = !isStarted
            if (isStarted) {
                gameJob = CoroutineScope(Main).launch {
                    gameLoop()
                }
            }
        }

        resetBtn.setOnClickListener {
            resetMeteors()
        }

        gameLayout.setOnTouchListener { view, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                gameJob?.cancel()

                createBullet(motionEvent.x, motionEvent.y)
                gameJob = CoroutineScope(Main).launch {
                    gameLoop()
                }
            }

            return@setOnTouchListener true
        }

    }

    private suspend fun gameLoop() {
        while (isStarted) {
            updateFrame()
            delay(1000/GAME_FPS)
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
                    -50f,
                    meteorView
                )
            )
            gameLayout.addView(meteors[i].meteorView)
            meteors[i].meteorView.layoutParams.height = meteors[i].size
            meteors[i].meteorView.layoutParams.width = meteors[i].size
            meteors[i].meteorView.x = meteors[i].currentX
            meteors[i].meteorView.y = meteors[i].currentY

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
                meteor.moveMeteor()
            } else if (meteor.meteorView.x < 0f) {
                meteor.currentX = 0f
                meteor.direction = PI.toFloat() - meteor.direction
                if (meteor.direction < 0) {
                    meteor.direction += 2 * PI.toFloat()
                }
                meteor.moveMeteor()
            } else if (meteor.meteorView.y - 150f > displayMetrics.heightPixels.toFloat()) {
                meteor.currentY = -50f
                meteor.moveMeteor()
            } else {
                meteor.moveMeteor()
            }

        }

        for (bullet in bullets) {
            bullet.moveBullet()
            if (bullet.bulletView.y + bullet.bulletView.layoutParams.height < 0f) {
                gameLayout.removeView(bullet.bulletView)
                bullets.remove(bullet)
                break
            }

        }
    }

    private fun resetMeteors() {
        for (meteor in meteors) {
            meteor.currentX = initialMeteorX
            meteor.currentY = initialMeteorY
        }
    }

    private fun createBullet(bulletX: Float, bulletY: Float) {
        val bulletView = ShapeableImageView(this)
        bulletView.id = View.generateViewId()
        bulletView.background =
            ContextCompat.getDrawable(applicationContext, R.drawable.ic_bullet_blue_24dp)

        val bullet = Bullet(
            10f,
            5,
            bulletX,
            bulletY,
            bulletView
        )
        bullets.add(bullet)
        val index = bullets.indexOf(bullet)
        gameLayout.addView(bullets[index].bulletView)
        bullets[index].setBullet()
        println(bullets.size)
    }
}


