package com.example.gra

import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Rect
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import kotlin.random.Random

enum class ObjectType {
    POSITIVE,
    POSITIVE2,
    POSITIVE3,
    POSITIVE4,
    POSITIVE5,
    LIFE_BOOST,
    NEGATIVE,
    ENLARGE,
    SHRINK,
    RANDOM,
    MAGNET
}

class GameActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager

    private lateinit var gameLayout: ConstraintLayout
    private lateinit var basket: ImageView

    private lateinit var scoreText: TextView
    private lateinit var livesText: TextView

    private var score = 0
    private var lives = 3

    private var basketX = 0f

    private val fallingObjects = mutableListOf<FallingObject>()

    private val gameHandler = Handler(Looper.getMainLooper())
    private val spawnHandler = Handler(Looper.getMainLooper())

    private var screenWidth = 0
    private var screenHeight = 0

    private var isBasketEnlarged = false
    private var isBasketShrunk = false

    private var normalBasketWidth = 0
    private var normalBasketHeight = 0
    private var enlargedBasketWidth = 0
    private var enlargedBasketHeight = 0
    private var shrunkenBasketWidth = 0
    private var shrunkenBasketHeight = 0

    private val enlargeShrinkDurationMs = 15_000L
    private val basketSizeHandler = Handler(Looper.getMainLooper())

    private var randomEffectActive = false
    private var wiggleModeActive = false
    private var speedFactor = 1.0f
    private var doublePointsActive = false
    private val randomEffectHandler = Handler(Looper.getMainLooper())

    private var speedGrowthFactor = 1.0f
    private val speedGrowthHandler = Handler(Looper.getMainLooper())
    private val speedGrowthIntervalMs = 10_000L

    private var magnetActive = false
    private val magnetHandler = Handler(Looper.getMainLooper())
    private val magnetDurationMs = 10_000L

    private var mediaPlayer: MediaPlayer? = null

    private var collectSound: MediaPlayer? = null
    private var hurtSound: MediaPlayer? = null

    data class FallingObject(
        val imageView: ImageView,
        val type: ObjectType,
        var xPos: Float,
        var yPos: Float,
        val baseSpeed: Float
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(R.layout.activity_game)

        gameLayout = findViewById(R.id.gameLayout)
        basket = findViewById(R.id.basket)
        scoreText = findViewById(R.id.scoreText)
        livesText = findViewById(R.id.livesText)
        updateScoreText()
        updateLivesText()

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        screenWidth = resources.displayMetrics.widthPixels
        screenHeight = resources.displayMetrics.heightPixels

        basket.post {
            normalBasketWidth = basket.width
            normalBasketHeight = basket.height

            enlargedBasketWidth = (normalBasketWidth * 1.5).toInt()
            enlargedBasketHeight = (normalBasketHeight * 1.5).toInt()

            shrunkenBasketWidth = (normalBasketWidth * 0.5).toInt()
            shrunkenBasketHeight = (normalBasketHeight * 0.5).toInt()

            basketX = (screenWidth - basket.width) / 2f
            basket.x = basketX
        }

        mediaPlayer = MediaPlayer.create(this, R.raw.background_music).apply {
            isLooping = true
        }

        collectSound = MediaPlayer.create(this, R.raw.collect)  // collect.mp3
        hurtSound = MediaPlayer.create(this, R.raw.hurt)        // hurt.mp3
    }

    override fun onResume() {
        super.onResume()
        mediaPlayer?.start()

        sensorManager.registerListener(
            this,
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_GAME
        )

        gameHandler.post(gameLoopRunnable)
        spawnHandler.post(spawnRunnable)
        speedGrowthHandler.post(speedGrowthRunnable)
    }

    override fun onPause() {
        super.onPause()
        gameHandler.removeCallbacks(gameLoopRunnable)
        spawnHandler.removeCallbacks(spawnRunnable)
        speedGrowthHandler.removeCallbacks(speedGrowthRunnable)

        sensorManager.unregisterListener(this)

        mediaPlayer?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()

        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null

        collectSound?.release()
        collectSound = null

        hurtSound?.release()
        hurtSound = null
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                basketX += it.values[0] * -5
                val leftBoundary = 0f
                val rightBoundary = screenWidth - basket.width
                basketX = basketX.coerceIn(leftBoundary, rightBoundary.toFloat())
                basket.x = basketX
                updateBasketImage()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { }

    private val gameLoopRunnable = object : Runnable {
        override fun run() {
            updateGame()
            gameHandler.postDelayed(this, 16)
        }
    }

    private fun updateGame() {
        val iterator = fallingObjects.iterator()
        while (iterator.hasNext()) {
            val obj = iterator.next()

            val effectiveSpeed = obj.baseSpeed * speedFactor * speedGrowthFactor
            obj.yPos += effectiveSpeed
            obj.imageView.y = obj.yPos

            if (wiggleModeActive) {
                val deltaX = (Random.nextFloat() * 20f) - 10f
                obj.xPos += deltaX
                if (obj.xPos < 0f) obj.xPos = 0f
                if (obj.xPos > screenWidth - obj.imageView.width) {
                    obj.xPos = (screenWidth - obj.imageView.width).toFloat()
                }
                obj.imageView.x = obj.xPos
            }

            if (magnetActive) {
                if (obj.type in listOf(
                        ObjectType.POSITIVE, ObjectType.POSITIVE2,
                        ObjectType.POSITIVE3, ObjectType.POSITIVE4, ObjectType.POSITIVE5
                    )
                ) {
                    val basketCenterX = basketX + basket.width / 2f
                    val objCenterX = obj.xPos + obj.imageView.width / 2f
                    val diffX = basketCenterX - objCenterX
                    obj.xPos += diffX * 0.1f
                    if (obj.xPos < 0f) obj.xPos = 0f
                    if (obj.xPos > screenWidth - obj.imageView.width) {
                        obj.xPos = (screenWidth - obj.imageView.width).toFloat()
                    }
                    obj.imageView.x = obj.xPos
                }
            }

            if (obj.yPos > screenHeight) {
                gameLayout.removeView(obj.imageView)
                iterator.remove()
                continue
            }

            if (checkCollision(obj.imageView, basket)) {
                when (obj.type) {
                    ObjectType.NEGATIVE -> {
                        // Hurt dźwięk
                        hurtSound?.start()

                        lives--
                        if (lives < 0) lives = 0
                        updateLivesText()
                        if (lives == 0) {
                            endGame()
                            return
                        }
                    }
                    else -> {
                        collectSound?.start()

                        when (obj.type) {
                            ObjectType.POSITIVE -> {
                                score += if (doublePointsActive) 2 else 1
                            }
                            ObjectType.POSITIVE2 -> {
                                score += if (doublePointsActive) 4 else 2
                            }
                            ObjectType.POSITIVE3 -> {
                                score += if (doublePointsActive) 6 else 3
                            }
                            ObjectType.POSITIVE4 -> {
                                score += if (doublePointsActive) 10 else 5
                            }
                            ObjectType.POSITIVE5 -> {
                                score += if (doublePointsActive) 20 else 10
                            }
                            ObjectType.LIFE_BOOST -> {
                                lives++
                            }
                            ObjectType.ENLARGE -> {
                                if (isBasketShrunk) {
                                    revertBasketSize()
                                } else {
                                    enlargeBasket()
                                }
                            }
                            ObjectType.SHRINK -> {
                                if (isBasketEnlarged) {
                                    revertBasketSize()
                                } else {
                                    shrinkBasket()
                                }
                            }
                            ObjectType.RANDOM -> {
                                applyRandomEffect()
                            }
                            ObjectType.MAGNET -> {
                                applyMagnet()
                            }
                            ObjectType.NEGATIVE -> TODO()
                        }
                        updateScoreText()
                        updateLivesText()
                    }
                }
                gameLayout.removeView(obj.imageView)
                iterator.remove()
            }
        }
    }

    private fun endGame() {
        gameHandler.removeCallbacks(gameLoopRunnable)
        spawnHandler.removeCallbacks(spawnRunnable)
        speedGrowthHandler.removeCallbacks(speedGrowthRunnable)
        magnetHandler.removeCallbacksAndMessages(null)

        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null

        collectSound?.release()
        collectSound = null
        hurtSound?.release()
        hurtSound = null

        val intent = Intent(this, GameOverActivity::class.java)
        intent.putExtra("FINAL_SCORE", score)
        startActivity(intent)
        finish()
    }

    private val spawnRunnable = object : Runnable {
        override fun run() {
            spawnRandomObject()
            spawnHandler.postDelayed(this, 2000)
        }
    }

    private fun spawnRandomObject() {
        val roll = (1..200).random()

        val type = when (roll) {
            in 1..2 -> ObjectType.LIFE_BOOST
            in 3..7 -> ObjectType.ENLARGE
            in 8..12 -> ObjectType.SHRINK
            in 13..17 -> ObjectType.MAGNET
            in 18..27 -> ObjectType.RANDOM
            in 28..77 -> ObjectType.NEGATIVE
            in 78..102 -> ObjectType.POSITIVE
            in 103..127 -> ObjectType.POSITIVE2
            in 128..151 -> ObjectType.POSITIVE3
            in 152..175 -> ObjectType.POSITIVE4
            else -> ObjectType.POSITIVE5
        }

        val imageView = ImageView(this)
        when (type) {
            ObjectType.POSITIVE -> imageView.setImageResource(R.drawable.ic_positive)
            ObjectType.POSITIVE2 -> imageView.setImageResource(R.drawable.ic_positive2)
            ObjectType.POSITIVE3 -> imageView.setImageResource(R.drawable.ic_positive3)
            ObjectType.POSITIVE4 -> imageView.setImageResource(R.drawable.ic_positive4)
            ObjectType.POSITIVE5 -> imageView.setImageResource(R.drawable.ic_positive5)
            ObjectType.LIFE_BOOST -> imageView.setImageResource(R.drawable.ic_life_boost)
            ObjectType.NEGATIVE -> imageView.setImageResource(R.drawable.ic_negative)
            ObjectType.ENLARGE  -> imageView.setImageResource(R.drawable.ic_enlarge)
            ObjectType.SHRINK   -> imageView.setImageResource(R.drawable.ic_shrink)
            ObjectType.RANDOM   -> imageView.setImageResource(R.drawable.ic_random)
            ObjectType.MAGNET   -> imageView.setImageResource(R.drawable.ic_magnet)
        }

        val size = 100
        val layoutParams = ConstraintLayout.LayoutParams(size, size)
        gameLayout.addView(imageView, layoutParams)

        val xPos = Random.nextInt(0, screenWidth - size).toFloat()
        val yPos = -size.toFloat()

        imageView.x = xPos
        imageView.y = yPos

        val baseSpeed = Random.nextInt(6, 12).toFloat()

        fallingObjects.add(
            FallingObject(
                imageView = imageView,
                type = type,
                xPos = xPos,
                yPos = yPos,
                baseSpeed = baseSpeed
            )
        )
    }

    private fun enlargeBasket() {
        if (isBasketShrunk) {
            revertBasketSize()
            return
        }
        if (!isBasketEnlarged) {
            isBasketEnlarged = true
            val lp = basket.layoutParams as ConstraintLayout.LayoutParams
            lp.width = enlargedBasketWidth
            lp.height = enlargedBasketHeight
            basket.layoutParams = lp

            basketX = basketX.coerceIn(0f, screenWidth - basket.width.toFloat())
            basket.x = basketX

            basketSizeHandler.postDelayed({ revertBasketSize() }, enlargeShrinkDurationMs)
        }
    }

    private fun shrinkBasket() {
        if (isBasketEnlarged) {
            revertBasketSize()
            return
        }
        if (!isBasketShrunk) {
            isBasketShrunk = true
            val lp = basket.layoutParams as ConstraintLayout.LayoutParams
            lp.width = shrunkenBasketWidth
            lp.height = shrunkenBasketHeight
            basket.layoutParams = lp

            basketX = basketX.coerceIn(0f, screenWidth - basket.width.toFloat())
            basket.x = basketX

            basketSizeHandler.postDelayed({ revertBasketSize() }, enlargeShrinkDurationMs)
        }
    }

    private fun revertBasketSize() {
        if (isBasketEnlarged || isBasketShrunk) {
            isBasketEnlarged = false
            isBasketShrunk = false

            val lp = basket.layoutParams as ConstraintLayout.LayoutParams
            lp.width = normalBasketWidth
            lp.height = normalBasketHeight
            basket.layoutParams = lp

            basketX = basketX.coerceIn(0f, screenWidth - basket.width.toFloat())
            basket.x = basketX
        }
    }

    private fun applyRandomEffect() {
        if (randomEffectActive) {
            revertRandomEffect()
        }
        randomEffectActive = true

        val effect = (1..4).random()
        when (effect) {
            1 -> wiggleModeActive = true
            2 -> speedFactor = 1.5f
            3 -> speedFactor = 0.5f
            4 -> doublePointsActive = true
        }
        randomEffectHandler.postDelayed({ revertRandomEffect() }, 10_000)
    }

    private fun revertRandomEffect() {
        wiggleModeActive = false
        speedFactor = 1.0f
        doublePointsActive = false

        randomEffectHandler.removeCallbacksAndMessages(null)
        randomEffectActive = false
    }

    private fun applyMagnet() {
        if (magnetActive) {
            revertMagnet()
        }
        magnetActive = true
        magnetHandler.postDelayed({ revertMagnet() }, magnetDurationMs)
    }

    private fun revertMagnet() {
        magnetActive = false
        magnetHandler.removeCallbacksAndMessages(null)
    }

    private val speedGrowthRunnable = object : Runnable {
        override fun run() {
            speedGrowthFactor += 0.01f
            speedGrowthHandler.postDelayed(this, speedGrowthIntervalMs)
        }
    }

    private fun updateBasketImage() {
        val maxX = (screenWidth - basket.width).coerceAtLeast(1)
        val relativeX = basketX / maxX

        when {
            relativeX < 0.2 -> basket.setImageResource(R.drawable.ic_basket_left2)
            relativeX < 0.4 -> basket.setImageResource(R.drawable.ic_basket_left)
            relativeX < 0.6 -> basket.setImageResource(R.drawable.ic_basket)
            relativeX < 0.8 -> basket.setImageResource(R.drawable.ic_basket_right)
            else -> basket.setImageResource(R.drawable.ic_basket_right2)
        }
    }

    private fun checkCollision(iv1: ImageView, iv2: ImageView): Boolean {
        val r1 = Rect(
            iv1.x.toInt(),
            iv1.y.toInt(),
            (iv1.x + iv1.width).toInt(),
            (iv1.y + iv1.height).toInt()
        )
        val r2 = Rect(
            iv2.x.toInt(),
            iv2.y.toInt(),
            (iv2.x + iv2.width).toInt(),
            (iv2.y + iv2.height).toInt()
        )
        return r1.intersect(r2)
    }

    private fun updateScoreText() {
        scoreText.text = "Score: $score"
    }

    private fun updateLivesText() {
        livesText.text = "Lives: $lives"
    }
}
