package com.ricko.firecracker

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.imageview.ShapeableImageView
import com.ricko.firecracker.objects.Bullet
import com.ricko.firecracker.objects.Meteor
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Main
import kotlin.math.PI
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private val meteors: ArrayList<Meteor> = ArrayList()
    private val bullets: ArrayList<Bullet> = ArrayList()

    private val displayMetrics = DisplayMetrics()
    private var isStarted = false
    private var gameJob: Job? = null
    private var initialMeteorX = 0f
    private var initialMeteorY = -50f

    companion object {
        const val NUMBER_OF_METEORS = 100
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

        newGameBtn.setOnClickListener {
            if (meteors.size > 0) {
                isStarted = !isStarted
                if (isStarted) {
                    gameJob = CoroutineScope(Main).launch {
                        gameLoop()
                    }
                }
            }
        }

        resetBtn.setOnClickListener {
            for (meteor in meteors) {
                gameLayout.removeView(meteor.meteorView)
            }
            meteors.clear()
            createMeteors()
        }

        gameLayout.setOnTouchListener { _, motionEvent ->
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
            val startFrameTime = System.currentTimeMillis()
            updateFrame()
            val frameDuration = System.currentTimeMillis() - startFrameTime
            val frameDelay =
                if ((1000 / GAME_FPS) - frameDuration > 0) (1000 / GAME_FPS) - frameDuration else 0
            if (meteors.size == 0) {
                Toast.makeText(applicationContext, "jej you win", Toast.LENGTH_LONG).show()
                gameJob?.cancel()
            }
            delay(frameDelay)
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
                    Random.nextInt(1, 2) * 40,
                    Random.nextInt(1, 10).toFloat(),
                    Random.nextInt(30, 150) * PI.toFloat() / 180,
                    Random.nextLong(1, 100),
                    initialMeteorX,
                    initialMeteorY,
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
        meteorLoop@ for (meteor in meteors) {
            when {
                meteor.meteorView.x + meteor.meteorView.layoutParams.width > displayMetrics.widthPixels.toFloat() -> {
                    meteor.currentX =
                        displayMetrics.widthPixels.toFloat() - meteor.meteorView.layoutParams.width
                    meteor.direction = PI.toFloat() - meteor.direction
                    if (meteor.direction > 2 * PI.toFloat()) {
                        meteor.direction -= 2 * PI.toFloat()
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
                meteor.meteorView.y - 150f > displayMetrics.heightPixels.toFloat() -> {
                    meteor.currentY = -50f
                    meteor.moveMeteor()
                }
                else -> {
                    meteor.moveMeteor()
                }
            }

            for (bullet in bullets) {
                if (bullet.bulletView.y in meteor.currentY..meteor.currentY + meteor.meteorView.layoutParams.height &&
                    bullet.bulletView.x + bullet.bulletView.layoutParams.width / 2 in meteor.currentX..meteor.currentX + meteor.meteorView.layoutParams.width
                ) {
                    gameLayout.removeView(bullet.bulletView)
                    gameLayout.removeView(meteor.meteorView)
                    bullets.remove(bullet)
                    meteors.remove(meteor)
                    break@meteorLoop
                }
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
    }
}


