package ru.glorient.granitbk_n.camera.opengl

import android.graphics.*
import android.graphics.drawable.Drawable
import ru.glorient.granitbk_n.MainActivity
import ru.glorient.granitbk_n.camera.opengl.utils.TimeUtils
import java.util.*

class CanvasDrawable : Drawable() {
    private var prevTimestamp = TimeUtils.now()
    private var step = POS_STEP
    private var posX = 0f
    private var posY = 0f

    private val backgroundPaint = Paint().apply {
        color = Color.GREEN
    }

    private val circlePaint = Paint().apply {
        isAntiAlias = true
        color = Color.RED
    }

    override fun draw(canvas: Canvas) {
//        val currentTimestamp = TimeUtils.now()
//        canvas.drawRect(bounds, backgroundPaint)
//        val width = bounds.width()
//        val height = bounds.height()
//
//        if (currentTimestamp - prevTimestamp > FRAME_DURATION) {
//            val ration = width.toFloat() / height.toFloat()
//
//            when {
//                posX > width -> {
//                    step = -POS_STEP
//                }
//
//                posX < 0 -> {
//                    step = POS_STEP
//                }
//            }
//
//            posX += step.toFloat()
//            posY = (posX / ration)
//        }
//
//        canvas.drawCircle(posX, posY, 0.1f * width, circlePaint)

        val paint = Paint()
//        paint.strokeWidth = 5f // set stroke width
        paint.textSize = 15f // set font size
        paint.color = Color.WHITE // set color
        //Get the canvas through lockcanvas
        //Fill the color of the canvas
        canvas.drawColor(
            PixelFormat.TRANSPARENT,
            PorterDuff.Mode.CLEAR
        ) // parameter 1: set to transparent, parameter 2: set to transparent PorterDuff.Mode.CLEAR : the paint will not be submitted to the canvas
        //Set up barrage content
        val text = "${getCurrentTime()}       ${MainActivity.locationStr}"
        canvas.drawText(text, 5f, 20f, paint)
    }

    // Получаем время
    private fun getCurrentTime(): String? {
        val calendar = Calendar.getInstance()
        val hour = calendar[Calendar.HOUR_OF_DAY]
        val minute = calendar[Calendar.MINUTE]
        val second = calendar[Calendar.SECOND]
        val day = calendar[Calendar.DATE]
        val month = calendar[Calendar.MONTH]
        val year = calendar[Calendar.YEAR]
        return String.format(
            "%02d.%02d.%02d  %02d:%02d:%02d",
            day, month + 1, year, hour, minute, second
        ) // ДД.ММ.ГГГГ ЧЧ:ММ:СС - формат времени
    }

    override fun setAlpha(alpha: Int) {
        backgroundPaint.alpha = alpha
    }

    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        backgroundPaint.colorFilter = colorFilter
    }

    private companion object {
        const val FRAME_DURATION = 60
        const val POS_STEP = 4
    }
}