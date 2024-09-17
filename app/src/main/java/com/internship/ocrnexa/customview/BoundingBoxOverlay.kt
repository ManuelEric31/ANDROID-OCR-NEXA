package com.internship.ocrnexa.customview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class BoundingBoxOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint: Paint = Paint().apply {
        color = Color.GREEN
        strokeWidth = 5f
        style = Paint.Style.STROKE
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val left = width * 0.1f
        val top = height * 0.3f
        val right = width * 0.9f
        val bottom = height * 0.6f
        canvas.drawRect(left, top, right, bottom, paint)
    }
}
