package com.ricko.firecracker

import android.graphics.Color
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.MotionEvent
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.ricko.firecracker.objects.Bullet
import com.ricko.firecracker.objects.Meteor
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sqrt
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private val meteors: ArrayList<Meteor> = ArrayList()
    private val bullets: ArrayList<Bullet> = ArrayList()

    private var numberOfMeteors =
        49                //** Should be lateinit and assigned based on level TODO
    private var barrierVerticalBias =
        .7f           //** Should be lateinit and assigned based on level TODO
    private var numberOfTaps =
        5                    //** Should be lateinit and assigned based on level TODO
    private var bulletSpeed =
        20f                   //** Should be lateinit and assigned based on level TODO
    private var maxMeteorSpeed =
        8                  //** Should be lateinit and assigned based on level TODO
    private var minMeteorSpeed =
        3                  //** Should be lateinit and assigned based on level TODO

    private val displayMetrics = DisplayMetrics()
    private var isStarted = false
    private var gameJob: Job? = null
    private var shootingJob: Job? = null
    private var initialMeteorX = 0f
    private var initialMeteorY = -500f
    private var isCriticalAreaActive = false
    private var activeExplosionCount = 0
    private var currentScore = 0
    private var meteorsCreated = 0

    companion object {
        const val MAX_NUMBER_OF_METEORS_IN_MOMENT = 25
        const val GAME_FPS = 58L
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

        adjustBarrier(barrierVerticalBias)

        newGameBtn.setOnClickListener {
            currentScore = 0
            meteorsCreated = 0
            removeRemainingMeteors()
            removeRemainingBullets()
            createMeteors(if (numberOfMeteors > MAX_NUMBER_OF_METEORS_IN_MOMENT) MAX_NUMBER_OF_METEORS_IN_MOMENT else numberOfMeteors)
            startGame()
        }

        pauseBtn.setOnClickListener {
            pauseGame()
        }

        resetBtn.setOnClickListener {
            currentScore = 0
            meteorsCreated = 0
            removeRemainingMeteors()
            removeRemainingBullets()
            createMeteors(if (numberOfMeteors > MAX_NUMBER_OF_METEORS_IN_MOMENT) MAX_NUMBER_OF_METEORS_IN_MOMENT else numberOfMeteors)
            startGame()
        }

        gameLayout.setOnTouchListener { _, motionEvent ->

            if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                shootingJob = CoroutineScope(Main).launch {
                    while (motionEvent.action == MotionEvent.ACTION_DOWN || motionEvent.action == MotionEvent.ACTION_MOVE) {
                        for (i in 0 until numberOfTaps) {
                            if (motionEvent.y > barrier.y) {
                                createBullet(motionEvent.x, motionEvent.y + i * 60)
                            }
                        }
                        delay(500)
                    }
                }

            } else if (motionEvent.action != MotionEvent.ACTION_DOWN && motionEvent.action != MotionEvent.ACTION_MOVE) {
                shootingJob?.cancel()
                shootingJob = null
            }

            return@setOnTouchListener true
        }

    }

    private fun adjustBarrier(verticalBias: Float) {
        val lParams = barrier.layoutParams as ConstraintLayout.LayoutParams
        lParams.verticalBias = verticalBias
        barrier.layoutParams = lParams
    }

    private fun startGame() {
        isStarted = !isStarted
        if (isStarted) {
            resetBtn.visibility = GONE
            newGameBtn.visibility = GONE
            pauseBtn.visibility = VISIBLE
            fpsCount.visibility = VISIBLE
            barrier.visibility = VISIBLE
            criticalArea.visibility = VISIBLE
            scoreCount.visibility = VISIBLE
            criticalArea.clearAnimation()
            gameJob = CoroutineScope(Main).launch {
                gameLoop()
            }
        } else {
            resetBtn.visibility = VISIBLE
            newGameBtn.visibility = VISIBLE
            pauseBtn.visibility = GONE
            fpsCount.visibility = GONE
            scoreCount.visibility = GONE
            barrier.visibility = GONE
            criticalArea.visibility = GONE
            criticalArea.clearAnimation()
        }
    }

    private fun pauseGame() {
        isStarted = !isStarted
        if (isStarted) {
            resetBtn.visibility = GONE
            gameJob = CoroutineScope(Main).launch {
                gameLoop()
            }
        } else resetBtn.visibility = VISIBLE
    }

    private fun gameOver() {
        isStarted = !isStarted
        resetBtn.visibility = VISIBLE
        criticalArea.clearAnimation()
    }

    private fun gameFinished() {
        Toast.makeText(applicationContext, "jej you win", Toast.LENGTH_LONG).show()
        isStarted = !isStarted
        gameJob?.cancel()
        gameJob = null
        newGameBtn.visibility = VISIBLE
        pauseBtn.visibility = GONE
        fpsCount.visibility = GONE
        barrier.visibility = GONE
        criticalArea.visibility = GONE
        criticalArea.clearAnimation()
        removeRemainingBullets()
    }

    private fun removeRemainingBullets() {
        for (bullet in bullets) {
            gameLayout.removeView(bullet.bulletView)
        }
        bullets.clear()
    }

    private fun removeRemainingMeteors() {
        for (meteor in meteors) {
            gameLayout.removeView(meteor.meteorView)
        }
        meteors.clear()
    }

    private suspend fun gameLoop() {
        var t = System.currentTimeMillis()
        while (isStarted) {
            val startFrameTime = System.currentTimeMillis()
            updateFrame()
            val frameDuration = System.currentTimeMillis() - startFrameTime
            val frameDelay =
                if ((1000 / GAME_FPS) - frameDuration > 0) (1000 / GAME_FPS) - frameDuration else 0
            if (meteors.size == 0) {
                gameFinished()
            }
            delay(frameDelay)
            if (System.currentTimeMillis() - t > 100) {
                val fps = 1000 / (System.currentTimeMillis() - startFrameTime)
                fpsCount.text = ("FPS $fps")
                t = System.currentTimeMillis()
            }

        }
    }

    private fun updateFrame() {
        val bulletsToRemove: ArrayList<Bullet> = arrayListOf()
        val meteorsToRemove: ArrayList<Meteor> = arrayListOf()
        if (meteors.size < 15 && meteorsCreated < numberOfMeteors) createMeteors()

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
                    gameOver()
                }
                else -> {
                    meteor.moveMeteor()
                }
            }

            for (bullet in bullets) {
                if (bullet.bulletView.y in meteor.currentY..meteor.currentY + meteor.meteorView.layoutParams.height &&
                    bullet.bulletView.x + bullet.bulletView.layoutParams.width / 2 in meteor.currentX..meteor.currentX + meteor.meteorView.layoutParams.width
                ) {
                    makeExplosion(
                        bullet.bulletView.x + bullet.bulletView.layoutParams.width / 2,
                        bullet.bulletView.y
                    )
                    bulletsToRemove.add(bullet)
                    meteorsToRemove.add(meteor)
                    gameLayout.removeView(bullet.bulletView)
                    gameLayout.removeView(meteor.meteorView)
                }
            }


        }

        for (bullet in bullets) {
            if (bullet.bulletView.y + bullet.bulletView.layoutParams.height < 0f) {
                gameLayout.removeView(bullet.bulletView)
                bulletsToRemove.add(bullet)
            } else bullet.moveBullet()
        }

        for (bulletToRemove in bulletsToRemove) {
            bullets.remove(bulletToRemove)
        }
        val meteorsOldSize = meteors.size
        for (meteorToRemove in meteorsToRemove) {
            meteors.remove(meteorToRemove)
        }

        currentScore += (meteorsOldSize - meteors.size)
        scoreCount.text = ("$currentScore/$numberOfMeteors")

        checkIfMeteorInCriticalArea()
    }

    private fun checkIfMeteorInCriticalArea() {
        var isMeteorInCriticalArea = false
        criticalAreaLoop@ for (meteor in meteors) {
            if (meteor.meteorView.y > barrier.y) {
                isMeteorInCriticalArea = true
                break@criticalAreaLoop
            } else {
                isMeteorInCriticalArea = false
            }
        }
        if (isMeteorInCriticalArea) {
            if (!isCriticalAreaActive) {
                criticalArea.setColorFilter(Color.RED)
                criticalArea.alpha = .2f
                criticalArea.startAnimation(AnimationUtils.loadAnimation(this, R.anim.blink))
                isCriticalAreaActive = true
            }
        } else {
            isCriticalAreaActive = false
            criticalArea.clearAnimation()
            criticalArea.alpha = .1f
            criticalArea.setColorFilter(R.color.colorPrimaryDark)
        }
    }

    private fun makeExplosion(locationX: Float, locationY: Float) {
        CoroutineScope(Main).launch {
            val explosionView = ImageView(this@MainActivity)
            explosionView.id = View.generateViewId()
            explosionView.background =
                ContextCompat.getDrawable(
                    applicationContext,
                    R.drawable.ic_fullscreen_exit_black_24dp
                )
            explosionView.elevation = 10f
            gameLayout.addView(explosionView)
            explosionView.y = locationY
            explosionView.x = locationX - explosionView.layoutParams.width / 2
            explosionView.layoutParams.width = 50
            explosionView.layoutParams.height = 50
            explosionView.animate().scaleXBy(2.5f).scaleYBy(2.5f).withEndAction {
                gameLayout.removeView(explosionView)
            }.duration = 100
            if (activeExplosionCount < 5) {
                launch {
                    withContext(IO) {
                        activeExplosionCount++
                        val sound = MediaPlayer.create(
                            this@MainActivity,
                            resources.getIdentifier("popping_sound", "raw", packageName)
                        )
                        sound.start()
                        delay(300)
                        sound.release()
                        activeExplosionCount--
                    }
                }
            }
        }
    }

    private fun createMeteors(howMany: Int = 1) {
        for (i in 0 until howMany) {
            meteorsCreated++
            val meteorView = ImageView(this)
            meteorView.id = View.generateViewId()
            meteorView.background =
                ContextCompat.getDrawable(applicationContext, R.drawable.ic_launcher_background)
            meteorView.elevation = 0f

            val meteor = Meteor(
                sqrt(Random.nextInt(1, 10).toFloat()).toInt() * 40,     //size
                Random.nextInt(minMeteorSpeed, maxMeteorSpeed).toFloat(),                //speed
                Random.nextInt(30, 150) * PI.toFloat() / 180,       //direction
                Random.nextLong(1, 100),                                     //health
                initialMeteorX,
                initialMeteorY,
                meteorView
            )
            meteors.add(meteor)
            val index = meteors.indexOf(meteor)
            gameLayout.addView(meteors[index].meteorView)
            meteors[index].meteorView.layoutParams.height = meteors[index].size
            meteors[index].meteorView.layoutParams.width = meteors[index].size
            meteors[index].meteorView.x = meteors[index].currentX
            meteors[index].meteorView.y = -meteors[index].size.toFloat()

        }
    }

    private fun createBullet(bulletX: Float, bulletY: Float) {
        val bulletView = ImageView(this@MainActivity)
        bulletView.id = View.generateViewId()
        bulletView.background =
            ContextCompat.getDrawable(applicationContext, R.drawable.ic_bullet_blue_24dp)

        val bullet = Bullet(
            bulletSpeed,
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


