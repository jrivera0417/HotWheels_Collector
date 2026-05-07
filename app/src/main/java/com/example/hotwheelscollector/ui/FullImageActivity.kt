package com.example.hotwheelscollector.ui

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.GestureDetector
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.hotwheelscollector.R

class FullImageActivity : AppCompatActivity(R.layout.activity_full_image) {

    private lateinit var img: ImageView
    private lateinit var btnBack: ImageView
    private lateinit var scaleDetector: ScaleGestureDetector
    private lateinit var gestureDetector: GestureDetector

    private var scaleFactor = 1f
    private var posX = 0f
    private var posY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var isDragging = false
    private var startY = 0f
    private var isSwipingDown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //FULLSCREEN
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        img = findViewById(R.id.fullImage)
        btnBack = findViewById(R.id.btnBack)

        btnBack.setOnClickListener {
            finish()
        }

        // =========================
        // IMAGE
        // =========================
        val base64 = intent.getStringExtra("IMAGE") ?: run {
            finish()
            return
        }

        val bytes = Base64.decode(base64, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        img.setImageBitmap(bitmap)

        // =========================
        // PINCH ZOOM
        // =========================
        scaleDetector = ScaleGestureDetector(this,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {

                    val prevScale = scaleFactor
                    scaleFactor *= detector.scaleFactor
                    scaleFactor = scaleFactor.coerceIn(1f, 5f)

                    val factor = scaleFactor / prevScale

                    img.scaleX *= factor
                    img.scaleY *= factor

                    return true
                }
            }
        )

        // =========================
        // DOUBLE TAP ZOOM
        // =========================
        gestureDetector = GestureDetector(this,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onDoubleTap(e: MotionEvent): Boolean {

                    scaleFactor = if (scaleFactor > 1f) 1f else 2.5f

                    if (scaleFactor == 1f) {
                        posX = 0f
                        posY = 0f
                    }

                    applyTransform()
                    return true
                }
            }
        )

        // =========================
        // TOUCH
        // =========================
        img.setOnTouchListener { _, event ->

            scaleDetector.onTouchEvent(event)
            gestureDetector.onTouchEvent(event)

            when (event.actionMasked) {

                MotionEvent.ACTION_DOWN -> {
                    lastX = event.x
                    lastY = event.y

                    startY = event.y
                    isSwipingDown = scaleFactor == 1f

                    isDragging = scaleFactor > 1f
                }

                MotionEvent.ACTION_MOVE -> {

                    if (isDragging) {

                        val dx = event.x - lastX
                        val dy = event.y - lastY

                        //sensibilidad ajustada al zoom
                        val zoomMultiplier = scaleFactor.coerceAtMost(3f)

                        posX += dx * zoomMultiplier
                        posY += dy * zoomMultiplier

                        applyTransform()

                        lastX = event.x
                        lastY = event.y
                    }

                    //SWIPE DOWN TO CLOSE
                    if (isSwipingDown && scaleFactor == 1f) {
                        val deltaY = event.y - startY

                        if (deltaY > 180) {
                            finish()
                        }
                    }
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {

                    isDragging = false
                    isSwipingDown = false

                    //rebote suave
                    img.animate()
                        .scaleX(scaleFactor)
                        .scaleY(scaleFactor)
                        .translationX(posX)
                        .translationY(posY)
                        .setDuration(180)
                        .start()
                }
            }

            true
        }
    }

    // =========================
    // APPLY TRANSFORM
    // =========================
    private fun applyTransform() {
        img.scaleX = scaleFactor
        img.scaleY = scaleFactor
        img.translationX = posX
        img.translationY = posY
    }

    override fun onDestroy() {
        super.onDestroy()

        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.show(WindowInsetsCompat.Type.systemBars())
    }
}