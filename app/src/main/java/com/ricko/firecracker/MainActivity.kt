package com.ricko.firecracker

import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.MotionEvent
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.ricko.firecracker.objects.Bullet
import com.ricko.firecracker.objects.Meteor
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sqrt
import kotlin.random.Random


class MainActivity : AppCompatActivity() {

    private val meteors: ArrayList<Meteor> = ArrayList()
    private val bullets: ArrayList<Bullet> = ArrayList()


    private var numberOfMeteors =
        25                              //TODO(Should be lateinit and assigned based on level)
    private var barrierVerticalBias =
        .7f                             //TODO(Should be lateinit and assigned based on level)
    private var gunHeatCapacity =
        352                             //TODO(Should be lateinit and assigned based on level)
    private var bulletSpeed =
        50f                             //TODO(Should be lateinit and assigned based on level)
    private var maxMeteorSpeed =
        20                              //TODO(Should be lateinit and assigned based on level 50f should be max bullet speed, depends on min size of meteor)
    private var minMeteorSpeed =
        1                               //TODO(Should be lateinit and assigned based on level)
    private var coolingSpeed =
        5                               //TODO(Should be lateinit and assigned based on level)
    private var fireRate =
        30L                             //TODO(Should be lateinit and assigned based on level)
    private var meteorMaxHealth =
        55L                             //TODO(Should be lateinit and assigned based on level)
    private var meteorMinHealth =
        50L                             //TODO(Should be lateinit and assigned based on level)

    private val coolingDelay = 50L

    private val displayMetrics = DisplayMetrics()
    private var isGameRunning = false
    private var whenPaused = 0L
    private var gameJob: Job? = null
    private var shootingJob: Job? = null
    private var stopShootingJob: Job? = null
    private var initialMeteorX = 0f
    private var initialMeteorY = -500f
    private var isCriticalAreaActive = false
    private var activeExplosionCount = 0
    private var activeHitCount = 0
    private var currentScore = 0
    private var meteorsCreated = 0
    private var currentBullet = 0

    private var overheatBiasHeatingJob: Job? = null
    private var overheatBiasCoolingJob: Job? = null

    companion object {
        const val MAX_NUMBER_OF_METEORS_IN_MOMENT = 25
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

        adjustBarrier(barrierVerticalBias)

        newGameBtn.setOnClickListener {
            currentScore = 0
            meteorsCreated = 0
            currentBullet = 0
            adjustOverheatBiasZero()
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
            currentBullet = 0
            adjustOverheatBiasZero()
            removeRemainingMeteors()
            removeRemainingBullets()
            createMeteors(if (numberOfMeteors > MAX_NUMBER_OF_METEORS_IN_MOMENT) MAX_NUMBER_OF_METEORS_IN_MOMENT else numberOfMeteors)
            startGame()
        }

        gameLayout.setOnTouchListener { _, motionEvent ->

            if (!isGameRunning && (System.currentTimeMillis() - whenPaused) > 1000) {
                resetBtn.visibility = GONE
                isGameRunning = !isGameRunning
                gameJob = CoroutineScope(Main).launch {
                    delay(1000)
                    gameLoop()
                }
            }

            if (motionEvent.action == MotionEvent.ACTION_DOWN && isGameRunning) {
                stopShootingJob?.cancel()
                stopShootingJob = null
                shootingJob = CoroutineScope(Main).launch {
                    outer@ while ((motionEvent.action == MotionEvent.ACTION_DOWN || motionEvent.action == MotionEvent.ACTION_MOVE) && isGameRunning) {
                        for (i in 0 until gunHeatCapacity) {
                            if (currentBullet < gunHeatCapacity) {
                                if (motionEvent.y > barrier.y) {
                                    createBullet(motionEvent.x, motionEvent.y)
                                }
                                currentBullet++
                                adjustOverheatBias(1f - currentBullet.toFloat() / gunHeatCapacity)
                                delay(fireRate)
                            } else break
                        }
                        break@outer
                    }
                    if (currentBullet >= gunHeatCapacity) delay(1000)
                    while (currentBullet > 0 && isGameRunning) {
                        currentBullet -= coolingSpeed
                        currentBullet = if (currentBullet < 0) 0 else currentBullet
                        adjustOverheatBias(1f - currentBullet.toFloat() / gunHeatCapacity)
                        delay(coolingDelay)
                    }
                }

            } else if (motionEvent.action != MotionEvent.ACTION_DOWN && motionEvent.action != MotionEvent.ACTION_MOVE) {
                shootingJob?.cancel()
                shootingJob = null
                stopShootingJob = CoroutineScope(Main).launch {
                    while (currentBullet > 0 && isGameRunning) {
                        currentBullet -= coolingSpeed
                        currentBullet = if (currentBullet < 0) 0 else currentBullet
                        adjustOverheatBias(1f - currentBullet.toFloat() / gunHeatCapacity)
                        delay(coolingDelay)
                    }
                }
            }

            return@setOnTouchListener true
        }

    }

    private fun adjustOverheatBiasZero() {
        overheatBiasHeatingJob?.cancel()
        overheatBiasCoolingJob?.cancel()
        val lParams = overheatView.layoutParams as ConstraintLayout.LayoutParams
        lParams.verticalBias = 1f
        overheatView.layoutParams = lParams
    }

    private suspend fun adjustOverheatBias(overheatBias: Float) {
        val lParams = overheatView.layoutParams as ConstraintLayout.LayoutParams
        var vBias = lParams.verticalBias
        val biasDiff = abs(overheatBias - vBias)

        overheatBiasHeatingJob?.cancel()
        overheatBiasCoolingJob?.cancel()

        if (vBias > overheatBias) {
            overheatBiasHeatingJob = CoroutineScope(Main).launch {
                while (vBias > overheatBias) {
                    vBias -= biasDiff / (fireRate - 1)
                    if (vBias < 0f) break
                    lParams.verticalBias = vBias
                    overheatView.layoutParams = lParams
                    delay(1)
                }
            }

        } else {
            overheatBiasCoolingJob = CoroutineScope(Main).launch {
                while (vBias < overheatBias) {
                    vBias += biasDiff / (coolingDelay - 1)
                    lParams.verticalBias = vBias
                    overheatView.layoutParams = lParams
                    delay(1)
                }
            }
        }

    }

    private fun adjustBarrier(verticalBias: Float) {
        val lParams = barrier.layoutParams as ConstraintLayout.LayoutParams
        lParams.verticalBias = verticalBias
        barrier.layoutParams = lParams
    }

    private fun startGame() {
        isGameRunning = !isGameRunning
        if (isGameRunning) {
            resetBtn.visibility = GONE
            newGameBtn.visibility = GONE
            pauseBtn.visibility = VISIBLE
            fpsCount.visibility = VISIBLE
            barrier.visibility = VISIBLE
            gunOverheatLayout.visibility = VISIBLE
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
            gunOverheatLayout.visibility = GONE
            criticalArea.visibility = GONE
            criticalArea.clearAnimation()
        }
    }

    private fun pauseGame() {
        whenPaused = System.currentTimeMillis()
        isGameRunning = false
        resetBtn.visibility = VISIBLE
    }

    private fun gameFinished() {
        val msg =
            if (currentScore.toFloat() / numberOfMeteors < 1f) "YEAH you win BUT YOU SUCK" else "YEAH you win"
        Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show()
        isGameRunning = !isGameRunning
        gameJob?.cancel()
        gameJob = null
        overheatBiasCoolingJob?.cancel()
        overheatBiasCoolingJob = null
        overheatBiasHeatingJob?.cancel()
        overheatBiasHeatingJob = null
        shootingJob?.cancel()
        shootingJob = null
        stopShootingJob?.cancel()
        stopShootingJob = null
        newGameBtn.visibility = VISIBLE
        pauseBtn.visibility = GONE
        fpsCount.visibility = GONE
        barrier.visibility = GONE
        gunOverheatLayout.visibility = GONE
        criticalArea.visibility = GONE
        criticalArea.clearAnimation()
        adjustOverheatBiasZero()
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
        while (isGameRunning) {
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
        if (meteors.size < MAX_NUMBER_OF_METEORS_IN_MOMENT && meteorsCreated < numberOfMeteors) createMeteors()

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
                    meteorsToRemove.add(meteor)
                    gameLayout.removeView(meteor.meteorView)
                    currentScore--
                }
                else -> {
                    meteor.moveMeteor()
                }
            }

            for (bullet in bullets) {
                if (bullet.bulletView.y in meteor.currentY..meteor.currentY + meteor.meteorView.layoutParams.height &&
                    bullet.bulletView.x + bullet.bulletView.layoutParams.width / 2 in meteor.currentX..meteor.currentX + meteor.meteorView.layoutParams.width
                ) {
                    if (meteor.health > 0) {
                        makeHittingMeteor(
                            bullet.bulletView.x + bullet.bulletView.layoutParams.width / 2,
                            meteor.meteorView.y + meteor.meteorView.layoutParams.height
                        )
                        meteor.health--
                        bulletsToRemove.add(bullet)
                        gameLayout.removeView(bullet.bulletView)
                    } else {
                        makeExplosion(
                            meteor.meteorView.x + meteor.meteorView.layoutParams.width / 2,
                            meteor.meteorView.y + meteor.meteorView.layoutParams.height / 2
                        )
                        meteorsToRemove.add(meteor)
                        bulletsToRemove.add(bullet)
                        gameLayout.removeView(meteor.meteorView)
                        gameLayout.removeView(bullet.bulletView)
                    }
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
        scoreCount.text = ("${floor(currentScore.toFloat() / numberOfMeteors * 1000) / 10}%")

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
                criticalArea.alpha = .15f
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

    private fun makeHittingMeteor(locationX: Float, locationY: Float) {
        CoroutineScope(Main).launch {
            //TODO(create hit animation)
//            val explosionView = ImageView(this@MainActivity)
//            explosionView.id = View.generateViewId()
//            explosionView.background =
//                ContextCompat.getDrawable(
//                    applicationContext,
//                    R.drawable.ic_fullscreen_exit_black_24dp
//                )
//            explosionView.elevation = 10f
//            gameLayout.addView(explosionView)
//            explosionView.y = locationY
//            explosionView.x = locationX - explosionView.layoutParams.width / 2
//            explosionView.layoutParams.width = 50
//            explosionView.layoutParams.height = 50
//            explosionView.animate().scaleXBy(2.5f).scaleYBy(2.5f).withEndAction {
//                gameLayout.removeView(explosionView)
//            }.duration = 100
            if (activeHitCount < 5) {
                launch {
                    withContext(IO) {
                        activeHitCount++
                        val sound = MediaPlayer.create(
                            this@MainActivity,
                            resources.getIdentifier("cans_hit", "raw", packageName)
                        )
                        sound.start()
                        delay(100)
                        sound.release()
                        activeHitCount--
                    }
                }
            }
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
                sqrt(Random.nextInt(1, 2).toFloat()).toInt() * 60,      //size
                Random.nextInt(minMeteorSpeed, maxMeteorSpeed).toFloat(),               //speed
                Random.nextInt(30, 150) * PI.toFloat() / 180,       //direction
                Random.nextLong(meteorMinHealth, meteorMaxHealth),                      //health
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