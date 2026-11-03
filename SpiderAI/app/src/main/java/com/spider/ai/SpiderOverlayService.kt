package com.spider.ai

import android.app.*
import android.content.*
import android.graphics.*
import android.graphics.drawable.*
import android.os.*
import android.provider.Settings
import android.view.*
import android.view.animation.*
import android.widget.*
import android.animation.*
import android.graphics.Paint
import kotlin.math.*

class SpiderOverlayService : Service() {

    private lateinit var wm: WindowManager
    private var spider: SpiderView? = null
    private var params: WindowManager.LayoutParams? = null

    override fun onBind(intent: Intent?) = null

    override fun onCreate() {
        super.onCreate()

        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        val channel = "spider_overlay"
        if (Build.VERSION.SDK_INT >= 26) {
            val ch = NotificationChannel(
                channel, "SPIDER AI Floating Logo",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(ch)
        }

        val notification = Notification.Builder(this, channel)
            .setContentTitle("SPIDER AI")
            .setContentText("Floating Spider is active")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                71, notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(71, notification)
        }

        wm = getSystemService(WINDOW_SERVICE) as WindowManager

        val size = (76 * resources.displayMetrics.density).toInt()

        params = WindowManager.LayoutParams(
            size,
            size,
            if (Build.VERSION.SDK_INT >= 26)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 35
            y = 220
        }

        spider = SpiderView(this)
        spider?.let { view ->
            wm.addView(view, params)
            animateSpider(view)
            enableDragging(view)
        }
    }

    private fun animateSpider(view: View) {
        ObjectAnimator.ofFloat(view, View.ROTATION_Y, 0f, 360f).apply {
            duration = 2800
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            start()
        }

        ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, 0f, -12f, 0f).apply {
            duration = 1800
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    private fun enableDragging(view: View) {
        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f
        var moved = false

        view.setOnTouchListener { _, event ->
            val p = params ?: return@setOnTouchListener false

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = p.x
                    startY = p.y
                    touchX = event.rawX
                    touchY = event.rawY
                    moved = false
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - touchX
                    val dy = event.rawY - touchY

                    if (abs(dx) > 5 || abs(dy) > 5) moved = true

                    p.x = startX + dx.toInt()
                    p.y = startY + dy.toInt()

                    try {
                        wm.updateViewLayout(view, p)
                    } catch (_: Exception) {}
                    true
                }

                MotionEvent.ACTION_UP -> {
                    if (!moved) {
                        val intent = Intent(this, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP
                        }
                        startActivity(intent)
                    }
                    true
                }

                else -> true
            }
        }
    }

    override fun onDestroy() {
        spider?.let {
            try { wm.removeView(it) } catch (_: Exception) {}
        }
        spider = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }
}

class SpiderView(context: Context) : View(context) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val s = min(width, height) / 100f
        canvas.save()
        canvas.translate(width / 2f, height / 2f)
        canvas.scale(s, s)

        // Spider legs
        p.style = Paint.Style.STROKE
        p.strokeWidth = 5f
        p.strokeCap = Paint.Cap.ROUND
        p.color = Color.rgb(235, 35, 65)

        for (side in listOf(-1f, 1f)) {
            for (i in 0..3) {
                val y = -24f + i * 16f
                val path = Path()
                path.moveTo(side * 12f, y)
                path.cubicTo(
                    side * 34f, y - 20f,
                    side * 38f, y + 5f,
                    side * 45f, y - 12f + i * 8f
                )
                canvas.drawPath(path, p)
            }
        }

        // Abdomen
        p.style = Paint.Style.FILL
        p.shader = RadialGradient(
            -10f, -5f, 35f,
            intArrayOf(
                Color.rgb(255, 100, 120),
                Color.rgb(190, 10, 40),
                Color.rgb(65, 0, 15)
            ),
            null, Shader.TileMode.CLAMP
        )
        canvas.drawOval(-32f, -30f, 32f, 30f, p)
        p.shader = null

        // Thorax and head
        p.color = Color.rgb(220, 20, 55)
        canvas.drawCircle(0f, 20f, 16f, p)
        p.color = Color.rgb(250, 45, 75)
        canvas.drawCircle(0f, 39f, 11f, p)

        // Eyes
        p.color = Color.WHITE
        canvas.drawCircle(-5f, 38f, 2.5f, p)
        canvas.drawCircle(5f, 38f, 2.5f, p)

        // Gloss highlight
        p.color = Color.argb(150, 255, 255, 255)
        canvas.drawOval(-20f, -22f, -5f, -10f, p)

        canvas.restore()
    }
}
