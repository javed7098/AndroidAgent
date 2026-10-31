package com.example.androidaiagent

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import android.view.MotionEvent
import android.widget.TextView

class JavedBubbleService : Service() {

    private lateinit var windowManager: WindowManager
    private var bubble: TextView? = null
    private var params: LayoutParams? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val view = TextView(this).apply {
            text = "J\n🎙"
            textSize = 17f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(
                    Color.rgb(0, 190, 255),
                    Color.rgb(100, 45, 230),
                    Color.rgb(220, 35, 180)
                )
            ).apply {
                shape = GradientDrawable.OVAL
                setStroke(3, Color.WHITE)
            }
            contentDescription = "Javed Control voice assistant"
            elevation = 12f
        }

        val lp = LayoutParams(
            dp(68),
            dp(68),
            LayoutParams.TYPE_APPLICATION_OVERLAY,
            LayoutParams.FLAG_NOT_FOCUSABLE or
                LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(12)
            y = dp(220)
        }

        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f
        var moved = false

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = lp.x
                    startY = lp.y
                    touchX = event.rawX
                    touchY = event.rawY
                    moved = false
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - touchX).toInt()
                    val dy = (event.rawY - touchY).toInt()
                    if (kotlin.math.abs(dx) > dp(5) ||
                        kotlin.math.abs(dy) > dp(5)) moved = true
                    lp.x = (startX + dx).coerceAtLeast(0)
                    lp.y = (startY + dy).coerceAtLeast(0)
                    try {
                        windowManager.updateViewLayout(view, lp)
                    } catch (_: Exception) {}
                    true
                }

                MotionEvent.ACTION_UP -> {
                    if (!moved) {
                        val launch = Intent(
                            this,
                            MainActivity::class.java
                        ).apply {
                            addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                            )
                            putExtra("JAVED_START_VOICE", true)
                        }
                        startActivity(launch)
                    }
                    true
                }

                else -> true
            }
        }

        try {
            windowManager.addView(view, lp)
            bubble = view
            params = lp
        } catch (_: Exception) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        bubble?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {}
        }
        bubble = null
        super.onDestroy()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
